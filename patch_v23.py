import os, shutil, sys

BASE = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
BAK = os.path.expanduser("~/pw_bak23/")
os.makedirs(BAK, exist_ok=True)

def read(n):
    with open(BASE + n, encoding="utf-8") as f:
        return f.read()

def write(n, t):
    shutil.copyfile(BASE + n, BAK + n)
    with open(BASE + n, "w", encoding="utf-8") as f:
        f.write(t)

def swap(text, old, new, label):
    c = text.count(old)
    if c != 1:
        print("STOP: could not find the spot for '" + label + "' (found " + str(c) + "). Nothing was changed.")
        sys.exit(1)
    return text.replace(old, new)

for n in ("Settings2.kt", "Scrobble.kt"):
    if not os.path.exists(BASE + n):
        print("STOP: " + n + " is missing. Unzip the v23 zip into your home folder first.")
        sys.exit(1)

sc = read("Screens.kt")
sc = swap(sc, "fun SettingsScreen(s: AppState) {", "private fun SettingsScreenOld(s: AppState) {", "old settings screen")

ma = read("MainActivity.kt")
ma = swap(ma, "            PagesHost(s)\n", "            PagesHost(s)\n            ScrobbleWatcher(s)\n", "scrobble watcher")

write("Screens.kt", sc)
write("MainActivity.kt", ma)
print("v23 applied. Now: cd ~/PrismWave && git add -A && git commit -m v23 && git push")
