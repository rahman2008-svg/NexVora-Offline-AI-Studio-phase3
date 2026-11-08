# Disabled upstream workflows

Files here are NOT executed by GitHub (only `.github/workflows/` is).

- `static.yml.disabled` - upstream "Deploy static content to Pages" workflow. It publishes the
  `skills/` folder to GitHub Pages on every push. It was disabled so that nothing is published
  by accident. To re-enable it deliberately, move it back to `.github/workflows/static.yml`.
- The upstream `build_android.yaml` (release build on every push/PR, signed with the debug key)
  was deleted and replaced by `android-debug.yml`, `android-release.yml`, `android-validation.yml`.
