import pathlib
import sys

root = pathlib.Path.home() / "PrismWave"
base = root / "app/src/main/java/com/prismwave/app"
ok = True
out = {}


def step(path, name, marker, old, new):
    global ok
    t = out.get(path) or path.read_text()
    if marker in t:
        print("OK    " + name + " (already applied)")
        return
    if old not in t:
        print("FAIL  " + name)
        ok = False
        return
    out[path] = t.replace(old, new, 1)
    print("OK    " + name)


step(base / "VideoBoost.kt", "update popup and news", "UpdateHost(",
     "    PlayerBridgeHook(s)\n", "    PlayerBridgeHook(s)\n    UpdateHost(s.accent)\n")

step(root / "app/src/main/AndroidManifest.xml", "install permission", "REQUEST_INSTALL_PACKAGES",
     '    <uses-permission android:name="android.permission.WAKE_LOCK" />\n',
     '    <uses-permission android:name="android.permission.WAKE_LOCK" />\n'
     '    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />\n')

step(root / "app/src/main/AndroidManifest.xml", "file provider for updates", "FILE_PROVIDER_PATHS",
     "        <service\n            android:name=\".PlaybackService\"",
     "        <provider\n"
     "            android:name=\"androidx.core.content.FileProvider\"\n"
     "            android:authorities=\"${applicationId}.fileprovider\"\n"
     "            android:exported=\"false\"\n"
     "            android:grantUriPermissions=\"true\">\n"
     "            <meta-data\n"
     "                android:name=\"android.support.FILE_PROVIDER_PATHS\"\n"
     "                android:resource=\"@xml/file_paths\" />\n"
     "        </provider>\n\n"
     "        <service\n            android:name=\".PlaybackService\"")

step(root / "app/build.gradle.kts", "version number from the GitHub build", "GITHUB_RUN_NUMBER",
     '        versionCode = 9\n        versionName = "0.6.1"\n',
     '        // every GitHub build gets a higher number, so the in-app updater knows when a newer one exists\n'
     '        val run = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0\n'
     '        versionCode = run + 100\n'
     '        versionName = "0.7." + run\n')

if ok:
    for p, t in out.items():
        p.write_text(t)
    print("\nAll good. Now: git add -A && git commit -m v38 && git push")
else:
    print("\nSomething did not match, nothing was changed. Send me this screen.")
    sys.exit(1)
