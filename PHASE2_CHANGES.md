# Phase 2 - Rebranding + Firebase Removal (NexVora Offline AI Studio)

NOT compiled in the authoring environment (no network). First real build = GitHub Actions.

## Identity
- namespace/applicationId: com.nexvora.offlineai ; versionName 1.0.0 ; versionCode 10000
- All sources moved to `java/com/nexvora/offlineai/` (549 files had package/import/proto/manifest refs rewritten)
- Deep-link scheme now `com.nexvora.offlineai://`; FileProvider authority = `${applicationId}.provider`
- App label `@string/app_name` = "NexVora Offline AI Studio"; rootProject.name updated

## Removed Firebase components
- Gradle: firebase-bom, firebase-analytics, firebase-messaging, google-services plugin (app + root + libs.versions.toml)
- Deleted file: `FcmMessagingService.kt` (GalleryFcmMessagingService)
- Manifest: FCM service, firebase default_notification_channel meta-data, Firebase Analytics receivers/services,
  c2dm / GSF / GET_ACCOUNTS permissions
- `Analytics.kt` rewritten as a local no-op sink (`localEvents`); `firebaseAnalytics` -> `localEvents` in 24 files
- `FirebaseApp.initializeApp` + collection toggle removed (GalleryApplication), `FirebaseAnalytics.Event.APP_OPEN` removed (MainActivity)
- Settings: analytics toggle, DataStore read/save functions, ViewModel functions, strings removed
- proto: `disable_firebase_analytics` (field 11) now `reserved`
- Helper renames: logErrorToFirebase->logLocalError, logToFirebase->logMetricsLocally, etc.
- Kept (not Firebase): WorkManager, local NotificationScheduleManager, LiteRT-LM, MediaPipe, ML Kit, AICore code

## Branding
- Color.kt: navy/cyan/violet palette (light + dark); default theme = dark
- New launcher icon (adaptive, navy bg + gradient mark), in-app logo, static splash icon; splash bg navy
- Legacy Google PNG launcher icons deleted (minSdk 31 uses adaptive icon)
- New `ui/about/AboutScreen.kt` + route `about` + button in Settings dialog
- `values-bn/strings.xml` (brand, welcome/privacy text, About screen) + `locales_config.xml` (per-app language en/bn)
- Terms/privacy dialog text rewritten (no Google ToS / analytics statement)

## Added files
.github/workflows/android-debug.yml, NOTICE, README.md (README.upstream.md = original), PHASE2_CHANGES.md,
res/xml/locales_config.xml, res/values-bn/strings.xml, ui/about/AboutScreen.kt

## Offline checks run
XML well-formedness (0 errors); every R.string used is defined; bn strings all exist in default;
no remaining `com.google.firebase` / `FirebaseAnalytics` references; heuristic import check (9 flagged = false positives).

## Known limitations
- Bangla covers brand/privacy/About only; remaining ~1,350 strings fall back to English.
- AICore only works for allowlisted Google package names -> AICore backend will not be available for this app ID.
- Model list still fetched from the official upstream GitHub allowlist (model_allowlists).
- Release build still signs with the debug key (upstream setting) - real signing comes with the release workflow.
- Upstream `build_android.yaml` (release build on push) is still present.
- Not done yet (later phases): Room chat history, dashboard, privacy center, writing studio, release/validation workflows.
