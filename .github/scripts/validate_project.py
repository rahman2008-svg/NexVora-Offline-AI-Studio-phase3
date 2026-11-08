#!/usr/bin/env python3
"""Offline project-configuration checks for NexVora Offline AI Studio.

Run from the repository root:  python .github/scripts/validate_project.py
Exit code 0 = all checks passed. This does NOT compile anything (CI does that).
"""
import glob, os, re, sys, tomllib
import xml.dom.minidom as minidom

NS = "com.nexvora.offlineai"
SRC = "Android/src"
APP = f"{SRC}/app"
MAIN = f"{APP}/src/main"
JAVA = f"{MAIN}/java/" + NS.replace(".", "/")
errors, notes = [], []

def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()

def check(cond, msg):
    (notes if cond else errors).append(("OK   " if cond else "FAIL ") + msg)

# 1. Gradle identity ----------------------------------------------------------
g = read(f"{APP}/build.gradle.kts")
check(f'namespace = "{NS}"' in g, f"namespace is {NS}")
check(f'applicationId = "{NS}"' in g, f"applicationId is {NS}")
check('versionName = "1.0.0"' in g, 'versionName is "1.0.0"')
check("versionCode = 10000" in g, "versionCode is 10000")
check("compileSdk" in g and "release(37)" in g, "compileSdk 37 (CI installs platforms;android-37.0)")
check("signingConfigs.getByName(\"debug\")" not in g, "release is not signed with the debug key")
check(not re.search(r'(storePassword|keyPassword)\s*=\s*"', g), "no hardcoded signing passwords")
check(f'"{NS}.GalleryApplication"' in g, "applicationName placeholder points to GalleryApplication")

# 2. Firebase / google-services fully removed ----------------------------------
needles = re.compile(r"firebase|google-services|google\.services|c2dm|FirebaseMessaging", re.I)
hits = []
for pat in (f"{SRC}/*.kts", f"{SRC}/gradle/*.toml", f"{APP}/*.kts", f"{MAIN}/AndroidManifest.xml"):
    for p in glob.glob(pat):
        if needles.search(read(p)):
            hits.append(p)
for p in glob.glob(f"{MAIN}/java/**/*.kt", recursive=True):
    if re.search(r"com\.google\.firebase|FirebaseApp|FirebaseAnalytics|FirebaseMessaging", read(p)):
        hits.append(p)
check(not hits, "no Firebase/google-services references in Gradle, manifest, or Kotlin" + (f" -> {hits}" if hits else ""))
check(not glob.glob("**/google-services.json", recursive=True), "no google-services.json committed")

# 3. Secrets / keystores not committed -----------------------------------------
bad = [p for ext in ("*.jks", "*.keystore", "*.p12") for p in glob.glob(f"**/{ext}", recursive=True)]
check(not bad, "no keystore files committed" + (f" -> {bad}" if bad else ""))

# 4. Package layout --------------------------------------------------------------
check(os.path.isdir(JAVA), f"source directory exists: {JAVA}")
check(not os.path.exists(f"{MAIN}/java/com/google"), "old com/google source tree is gone")
mismatch = []
for p in glob.glob(f"{MAIN}/java/**/*.kt", recursive=True):
    m = re.search(r"^package\s+([\w.]+)", read(p), re.M)
    expected = os.path.dirname(os.path.relpath(p, f"{MAIN}/java")).replace(os.sep, ".")
    if not m or m.group(1) != expected:
        mismatch.append(p)
check(not mismatch, "every Kotlin file's package matches its directory" + (f" -> {mismatch[:5]}" if mismatch else ""))
leftover = [p for p in glob.glob(f"{MAIN}/**/*.kt", recursive=True) + glob.glob(f"{MAIN}/proto/*.proto")
            if re.search(r"^(package|import|option java_package).*com\.google\.ai\.edge\.gallery", read(p), re.M)]
check(not leftover, "no package/import still uses com.google.ai.edge.gallery")

# 5. Manifest ----------------------------------------------------------------------
mf = read(f"{MAIN}/AndroidManifest.xml")
minidom.parseString(mf.encode("utf-8"))
check('android:label="@string/app_name"' in mf, "manifest label is @string/app_name")
check('<data android:scheme="com.nexvora.offlineai"' in mf, "deep-link scheme is com.nexvora.offlineai")
check("${applicationId}.provider" in mf, "FileProvider authority uses ${applicationId}")
for m in re.finditer(r'android:name="(\.[\w.]+|' + re.escape(NS) + r'\.[\w.]+)"', mf):
    n = m.group(1)
    full = NS + n if n.startswith(".") else n
    rel = full[len(NS) + 1:].replace(".", "/") + ".kt"
    simple = rel.split("/")[-1][:-3]
    d = os.path.dirname(f"{JAVA}/{rel}")
    ok = any(re.search(rf"\b(class|object)\s+{simple}\b", read(f)) for f in glob.glob(d + "/*.kt"))
    check(ok, f"manifest component exists: {full}")

# 6. XML + resources ------------------------------------------------------------------
xml_bad = []
for p in glob.glob(f"{MAIN}/res/**/*.xml", recursive=True):
    try:
        minidom.parse(p)
    except Exception as e:  # noqa
        xml_bad.append(f"{p}: {e}")
check(not xml_bad, "all res/*.xml files are well-formed" + (f" -> {xml_bad}" if xml_bad else ""))
defs = set()
for p in glob.glob(f"{MAIN}/res/values*/*.xml"):
    defs |= set(re.findall(r'<(?:string|plurals|string-array)\s+name="([^"]+)"', read(p)))
used = set()
for p in glob.glob(f"{MAIN}/java/**/*.kt", recursive=True):
    used |= set(re.findall(r"\bR\.string\.(\w+)", read(p)))
missing = sorted(used - defs)
check(not missing, "every R.string used in Kotlin is defined" + (f" -> {missing[:10]}" if missing else ""))
default = set(re.findall(r'<string name="([^"]+)"', read(f"{MAIN}/res/values/strings.xml")))
bn = set(re.findall(r'<string name="([^"]+)"', read(f"{MAIN}/res/values-bn/strings.xml")))
check(not (bn - default), "Bangla strings are a subset of the default strings")
check(os.path.exists(f"{MAIN}/res/xml/locales_config.xml"), "locales_config.xml present (en, bn)")

# 7. Version catalog ---------------------------------------------------------------------
t = tomllib.loads(read(f"{SRC}/gradle/libs.versions.toml"))
libs = {k.replace("-", ".").replace("_", ".") for k in t["libraries"]}
plugs = {k.replace("-", ".").replace("_", ".") for k in t["plugins"]}
ul, up = set(), set()
for p in (f"{APP}/build.gradle.kts", f"{SRC}/build.gradle.kts"):
    s = read(p)
    ul |= set(re.findall(r"libs\.(?!plugins\.|versions\.)([\w.]+?)(?=\)|\s|$)", s))
    up |= set(re.findall(r"libs\.plugins\.([\w.]+)", s))
check(ul <= libs, f"all libs.* aliases exist in the catalog (missing: {sorted(ul - libs)})")
check(up <= plugs, f"all libs.plugins.* aliases exist (missing: {sorted(up - plugs)})")

# 8. Gradle wrapper ------------------------------------------------------------------------
check(os.path.exists(f"{SRC}/gradlew") and os.path.exists(f"{SRC}/gradle/wrapper/gradle-wrapper.jar"),
      "gradlew and gradle-wrapper.jar present")
check("gradle-9.2.1" in read(f"{SRC}/gradle/wrapper/gradle-wrapper.properties"), "Gradle wrapper is 9.2.1")

# 8b. Phase 3: Room, tests, screens ---------------------------------------------------------
check("libs.room.runtime" in g and "ksp(libs.room.compiler)" in g, "Room dependencies + KSP compiler declared")
check('arg("room.schemaLocation"' in g, "Room schema export configured")
db = read(f"{JAVA}/data/db/NexvoraDatabase.kt")
check("version = 1" in db and "exportSchema = true" in db, "NexvoraDatabase version/exportSchema declared")
check(not re.search(r"fallbackToDestructiveMigration", db), "destructive migration is not enabled")
appmod = read(f"{JAVA}/di/AppModule.kt")
check("RoomChatSessionRepository" in appmod and "DefaultChatSessionRepository(" not in appmod,
      "ChatSessionRepository is bound to the Room implementation")
ents = read(f"{JAVA}/data/db/NexvoraEntities.kt")
check(all(t in ents for t in ("conversations", "messages", "model_metadata", "user_preferences")),
      "all four Room tables are declared")
for rel in ("dashboard/DashboardScreen", "history/HistoryScreen", "privacy/PrivacyScreen", "hub/ModelHubScreen",
            "settings/SettingsScreen", "tools/ToolsScreen", "tools/ProductivityViewModel"):
    check(os.path.exists(f"{JAVA}/ui/nexvora/{rel}.kt"), f"screen exists: {rel}")
nav = read(f"{JAVA}/ui/navigation/GalleryNavGraph.kt")
check("startDestination = ROUTE_DASHBOARD" in nav, "dashboard is the start destination")
for route in ("ROUTE_HISTORY", "ROUTE_PRIVACY", "ROUTE_MODEL_HUB", "ROUTE_SETTINGS", "ROUTE_TOOLS", "ROUTE_ABOUT"):
    check(re.search(r'route\s*=\s*"?\$?' + route, nav) is not None, f"route registered: {route}")
unit_tests = glob.glob(f"{APP}/src/test/**/*Test.kt", recursive=True)
check(len(unit_tests) >= 6, f"unit tests present ({len(unit_tests)} files)")
check(os.path.exists(f"{APP}/src/androidTest/java/com/nexvora/offlineai/NexvoraDatabaseTest.kt"),
      "instrumented Room test present")
tool_vm = read(f"{JAVA}/ui/nexvora/tools/ProductivityViewModel.kt")
check("runInference(" in tool_vm and "runtimeHelper" in tool_vm, "AI tools call the real runtime helper (no canned output)")

# 9. Workflows -----------------------------------------------------------------------------
try:
    import yaml
except ImportError:
    yaml = None
wf = sorted(glob.glob(".github/workflows/*.y*ml"))
names = {os.path.basename(p) for p in wf}
check({"android-debug.yml", "android-release.yml", "android-validation.yml"} <= names,
      "android-debug / android-release / android-validation workflows exist")
check(names <= {"android-debug.yml", "android-release.yml", "android-validation.yml"},
      f"no other active workflows (found: {sorted(names)})")
for p in wf:
    s = read(p)
    if yaml:
        try:
            yaml.safe_load(s)
            check(True, f"valid YAML: {p}")
        except Exception as e:  # noqa
            check(False, f"invalid YAML: {p}: {e}")
    check("java-version: \"21\"" in s, f"JDK 21 in {p}")
    check(not re.search(r"gh release|softprops/action-gh-release|deploy-pages|upload-pages-artifact", s),
          f"{p} cannot publish releases or pages")
    check("secrets." not in s or p.endswith("android-release.yml"), f"secrets only used in release workflow ({p})")

print("\n".join(notes))
if errors:
    print("\n".join(errors))
    print(f"\n{len(errors)} check(s) FAILED")
    sys.exit(1)
print(f"\nAll {len(notes)} checks passed")
