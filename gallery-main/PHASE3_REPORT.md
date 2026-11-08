# Phase 3 Report - Core Platform (NexVora Offline AI Studio)

**Honest status: source code written, NOT yet compiled.** The authoring environment has no network,
so Gradle could not download dependencies. Nothing below is claimed as "working" until the first
GitHub Actions run (Android Validation + Android Debug APK) is green and the app is tried on a phone.

## Design decision (read this first)
The upstream app already persisted chat history, through a `ChatSessionRepository` interface backed by
a proto DataStore. Instead of building a second, parallel chat system, Phase 3 **replaced the storage
behind that interface with Room**. The existing LiteRT-LM chat code, session manager, history drawer and
XR/agent features therefore save and restore through Room without any change to inference code.

## What was built
| Area | Implementation | Files |
|---|---|---|
| Room database | 4 entities (conversations, messages, model_metadata, user_preferences), 4 DAOs, `NexvoraDatabase` v1, schema export, migration scaffold (no destructive fallback) | `data/db/NexvoraEntities.kt`, `NexvoraDaos.kt`, `NexvoraDatabase.kt` |
| Repository | `RoomChatSessionRepository` implements the existing `ChatSessionRepository` (+ rename, search, summaries); lossless restore via stored `ChatMessageProto`; custom titles survive auto re-save; message timestamps kept stable | `data/db/RoomChatSessionRepository.kt`, `ChatSessionMapper.kt` |
| Preferences | Key/value prefs in Room (`save history`, `default model`, `language`, `tools target language`) | `data/db/PreferenceRepository.kt` |
| Model metadata | Installed state, size, runtime, first/last seen, last used, synced from the model manager | `data/db/ModelMetadataRepository.kt` |
| Legacy import | One-time copy of old proto-DataStore chats into Room, old data cleared only if all copied | `data/db/LegacyChatImporter.kt`, `GalleryApplication.kt` |
| Local data | Counts, DB size, models size, cache size; clear history; clear all local data (models kept) | `data/db/LocalDataManager.kt`, `data/LocalStorage.kt` |
| DI | Room + DAOs provided; `ChatSessionRepository` bound to Room | `di/DatabaseModule.kt`, `di/AppModule.kt` |
| Dashboard (new start screen) | New chat, recent chats, installed models, AI tools, storage overview, privacy status, quick navigation, TOS dialog kept | `ui/nexvora/dashboard/*` |
| Chat management | History list with search (title + message text), rename, delete (confirm), empty/no-result states, read-only transcript with timestamps, **Continue chat** (restores the session in the chat screen once the model has loaded) | `ui/nexvora/history/*`, `ChatView.kt` hook, `NexvoraCommon.kt` |
| Privacy Center | Local data stats, history on/off, clear history, clear all, permission list with live state, network explanation, "no analytics/tracking" | `ui/nexvora/privacy/*` |
| Model Hub | Installed / downloading / available groups, real progress + speed from the existing download manager, cancel, retry/resume, delete (confirm), download confirm, RAM + storage compatibility hints, storage summary | `ui/nexvora/hub/*` |
| Settings | Theme (system/light/dark), language (system/English/Bangla), save-history switch, default model for AI tools, About, Privacy, Model Hub, open-source licenses | `ui/nexvora/settings/*` |
| AI tools | Writing, Summarize, Translate, Study assistant, Rewrite, Notes. Runs on the installed local LLM through the existing `runtimeHelper.runInference` (same path as Prompt Lab). Results can be copied/shared and are saved to history. Model is released when leaving the screen (RAM). | `ui/nexvora/tools/*` |
| Strings | 158 new strings in English and Bangla | `res/values*/strings.xml` |
| Small upstream edits | `ChatSessionRepository` got `getChatSession` + `renameChatSession`; `DefaultLlmSessionManager` now loads one session instead of all sessions on each save | listed in test report |

## Not done / deliberately limited
- **No compile, no run, no device test.** See `PHASE3_TEST_REPORT.md`.
- Language switching works on **Android 13+ only** (per-app locale API). On Android 12 the buttons are disabled and a note is shown.
- Bangla covers all NexVora screens; the ~1,350 inherited upstream strings are still English.
- Tool conversations are saved to history and viewable, but **cannot be continued** in the chat screen.
- "Default model" applies to the AI tools only, not to the chat screen.
- Model Hub downloads use the existing download manager. Models that need a Hugging Face sign-in/licence show a message telling the user to open the model from its AI task screen. There is no Wi-Fi-only enforcement, only a confirm dialog.
- Room schema is v1; no migration exists yet (nothing to migrate). The CI uploads the exported schema JSON so it can be committed for future migration tests.
- `chatSessions` / `getAllChatSessions()` still load every session with its messages (same behaviour as upstream). With very large histories this costs RAM; list screens use lightweight queries instead.
- Vision/audio/other upstream tasks are untouched and reachable through "All AI tasks".
- Accessibility: content descriptions on icon buttons and button roles on clickable rows were added; **no TalkBack testing** was done.

## Risks to check on the first build
1. Room 2.8.0 / KSP 2.3.6 / AGP 9.0.1 / Hilt 2.60.1 combination resolving and processing together.
2. `SingleChoiceSegmentedButtonRow`, `LinearProgressIndicator(progress = {})`, `LocalClipboard` need the Compose Material3 version already used upstream (the same APIs are used elsewhere in the project).
3. Model initialization in the AI tools reuses the app's Prompt Lab task configuration; verify a real generation on a device.
4. "Continue chat" relies on the model finishing initialization; verify on a device.
