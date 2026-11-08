# NexVora Offline AI Studio

Privacy-focused, offline-first AI studio for Android.
Developed by **Prince AR Abdur Rahman** - published by **NexVora Lab's Ofc**.

- Application ID: `com.nexvora.offlineai` - Version `1.0.0` (code `10000`)
- Local on-device inference (LiteRT-LM, MediaPipe, ML Kit GenAI, AICore integrations preserved).
- No analytics, no tracking, no Firebase. Chats and files stay on the device.
- Internet is only needed to download models; downloaded models run offline.
- UI languages: English and Bangla (per-app language in Android settings).

## Build
Open `Android/src` in Android Studio (JDK 21, Android SDK Platform 37) or run
`cd Android/src && ./gradlew assembleDebug`.

GitHub Actions (`.github/workflows/`):
- `android-debug.yml` - builds the debug APK, artifact `NexVora-Offline-AI-Studio-debug-apk`.
- `android-validation.yml` - config checks + Kotlin compile (no APK).
- `android-release.yml` - manual, signed release APK from GitHub Secrets
  (`NEXVORA_KEYSTORE_BASE64`, `NEXVORA_KEYSTORE_PASSWORD`, `NEXVORA_KEY_ALIAS`, `NEXVORA_KEY_PASSWORD`).

Local config check (no Android SDK needed): `python .github/scripts/validate_project.py`.

## Features (Phase 3, awaiting first build)
- Dashboard: new chat, recent chats, installed models, AI tools, storage overview, privacy status.
- Chat history in a local Room database: search, rename, delete, continue a previous chat.
- Model Hub: download progress, cancel/retry, delete, RAM/storage compatibility hints.
- Privacy Center: local data sizes, clear history / all local data, permission overview.
- AI tools on the local LLM: writing, summarize, translate, study, rewrite, notes.
- Settings: theme, language (Android 13+), history on/off, default tool model.

## Status
This repository is being customized in phases. Phase 2 (rebranding + Firebase removal) and Phase 2.1 (build pipeline) are done; Phase 3 (core platform) is written but **not yet compiled or tested**.
See `PHASE2_CHANGES.md`, `PHASE2_1_CHANGES.md`, `PHASE3_REPORT.md`, `PHASE3_TEST_REPORT.md`. The project has **not** been compiled in the authoring environment (no network),
so the first GitHub Actions run is the first real build.

## Attribution
Based on Google AI Edge Gallery (Apache License 2.0). See `LICENSE` and `NOTICE`.
The original upstream README is preserved as `README.upstream.md`.
