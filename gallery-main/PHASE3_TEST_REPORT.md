# Phase 3 Test Report

## What was actually executed in the authoring environment
| Check | Result |
|---|---|
| `python .github/scripts/validate_project.py` (67 offline checks: identity, no Firebase, manifest, resources, string references, catalog aliases, workflows, Room config, screens, routes, tests present, AI tools call the real runtime) | **67 / 67 passed** |
| The DAO search SQL (`LIKE ... ESCAPE '\'` over conversations JOIN messages) executed against real SQLite: matches title and message text, `%` and `_` treated as literal characters, Bangla text matches, blank query produces no pattern, deleting a conversation cascades to its messages | **passed** |
| XML well-formedness of all resources; every `R.string` used in Kotlin is defined; Bangla strings are a subset of English | passed |
| Bracket balance + import-resolution scan of all new Kotlin files | passed (heuristic, not a compiler) |

## What was NOT executed (no network, no Android SDK, no emulator)
- Gradle build, Kotlin/KSP/Room/Hilt compilation  -> **unverified**
- The 42 JVM unit tests listed below -> **written, never run**
- The 11 instrumented Room tests -> **written, never run** (they need a device/emulator; GitHub workflows do not run them)
- GitHub Actions -> **not run**
- Real inference, model download, chat persistence on a device -> **not tested**
- TalkBack/accessibility, 6 GB RAM behaviour, offline behaviour -> **not tested**

## Tests added
JVM unit tests (`app/src/test/...`, run by the Validation workflow via `testDebugUnitTest`):
- `ChatSessionMapperTest` (14): proto<->entity round trip, timestamps kept/created, custom title kept, blank title, preview, title normalisation, corrupt payload fallback, role mapping
- `ProductivityPromptsTest` (10): blank input builds nothing, every tool embeds the user text, translate/rewrite/writing/study/notes options, Bangla instruction, input length cap, id round trip
- `ModelCompatibilityTest` (6): RAM rounding for "6 GB" phones, OK / low memory / not enough storage / unknown
- `LocalStorageTest` (4): byte formatting, directory size, clearing a directory
- `SearchQueryTest` (4): blank, trimming, `%`/`_`/`\` escaping, Bangla
- `ModelMetadataMergeTest` (2): new vs existing model merge
- `PendingChatRestoreTest` (2): request/clear, expiry

Instrumented tests (`app/src/androidTest/...`, `NexvoraDatabaseTest`, 11): save/load round trip, re-save keeps timestamps, rename persists across auto re-save, invalid rename rejected, delete cascades, clear all, search (title/message/wildcards), newest-first ordering, "history off" saves nothing, preference defaults, model metadata sync/mark-used.

## How to get real results
1. Push to GitHub, run **Android Validation** (compiles + runs the 42 unit tests; uploads `unit-test-reports` and `room-schemas`).
2. Run **Android Debug APK**, install, and walk through: new chat -> send a message -> kill app -> history shows it -> Continue; rename; search; Model Hub download/cancel/delete; each AI tool; Privacy Center clear; language switch (Android 13+).
3. Optional: `./gradlew :app:connectedDebugAndroidTest` on an emulator for the Room tests.
4. Send the failing step's log if anything is red; fix first, then Phase 4.
