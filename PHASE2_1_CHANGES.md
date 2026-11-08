# Phase 2.1 - Build Infrastructure and First APK Validation

NOT built yet: no network in the authoring environment. The first GitHub Actions run is the first real build.

## Inspection results (all offline)
- namespace / applicationId / manifest scheme / provider authority = com.nexvora.offlineai (consistent)
- Every Kotlin `package` matches its directory; no `com.google.ai.edge.gallery` package/import left
- Version catalog parses; every `libs.*` / `libs.plugins.*` alias used exists; no dangling version refs
- Manifest components (MainActivity, 2 receivers, GlassesTapActivity) resolve to real classes
- All res XML well-formed; every `R.string` used is defined; Bangla strings are a subset of English
- Gradle wrapper 9.2.1, AGP 9.0.1, Kotlin 2.2.21, compileSdk 37 (CI installs `platforms;android-37.0`), JDK 21

## Modified files
- Android/src/app/build.gradle.kts - env/-P based release signing (`nexvoraRelease`); release no longer uses the debug key
- Android/src/gradle.properties - Gradle heap 4 GB (done in Phase 2, kept)
- .github/workflows/android-debug.yml - rewritten (cache, SDK 37, rename + upload APK, summary, concurrency)
- README.md - build/CI/secrets documentation

## Added files
- .github/workflows/android-release.yml (manual, secrets-based signing, apksigner verify, no publishing)
- .github/workflows/android-validation.yml (config checks + compile + unit-test task)
- .github/scripts/validate_project.py (44 offline checks; runs locally and in CI)
- .github/workflows-disabled/README.md
- PHASE2_1_CHANGES.md

## Removed / disabled
- DELETED .github/workflows/build_android.yaml (upstream: release build on every push/PR, debug-key signed)
- DISABLED .github/workflows/static.yml -> .github/workflows-disabled/static.yml.disabled
  (upstream GitHub Pages deploy of `skills/` on every push)

## Not changed (AI functionality preserved)
No Kotlin source, inference, download, WorkManager, LiteRT-LM, MediaPipe, ML Kit or AICore code was touched in 2.1.

## Known build limitations / risks
- Never compiled here. Expect possible first-build errors; send the failing step's log.
- compileSdk 37 / AGP 9.0.1 / Gradle 9.2.1 are upstream values; they need `platforms;android-37.0` on the runner.
  If the runner image lacks it or a dependency cannot be resolved, the workflow fails at that step.
- Kotlin 2.2.21 with forced kotlin-metadata-jvm 2.4.0 is an upstream oddity; kept as-is.
- HuggingFace OAuth redirect scheme is still the upstream placeholder (only affects gated-model login).
- AICore backend unavailable for this application ID (Google allowlist).
- Release APK needs your own keystore + 4 secrets; without them the release workflow stops with a clear error.
- `testDebugUnitTest` has no tests (upstream has none in this tree); it passes trivially.
