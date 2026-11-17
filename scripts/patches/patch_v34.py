import pathlib
import re
import sys

base = pathlib.Path.home() / "PrismWave/app/src/main/java/com/prismwave/app"
ok = True
files = {}


def load(n):
    if n not in files:
        files[n] = (base / n).read_text()
    return files[n]


def say(name, good):
    global ok
    print(("OK    " if good else "FAIL  ") + name)
    if not good:
        ok = False


# ---------------- MainActivity.kt
t = load("MainActivity.kt")
# AI Studio button + page, and Star Struck, are removed from the screen
t2 = re.sub(r"(?m)^[ \t]*StudioHost\(s\)[ \t]*\n", "", t)
t2 = re.sub(r"(?m)^[ \t]*StarStruckHost\(s\)[ \t]*\n", "", t2)
say("AI Studio and Star Struck removed from the screen", "StudioHost(s)" not in t2 and "StarStruckHost(s)" not in t2)
# the new cat scene replaces the old cat
if "NavCatScene(s)" in t2:
    say("cat scene (already applied)", True)
else:
    n = re.sub(r"\bNavCat\(s\)", "NavCatScene(s)", t2, count=1)
    say("cat scene", n != t2)
    t2 = n
files["MainActivity.kt"] = t2

# ---------------- Settings2.kt: the switches live in Settings now
t = load("Settings2.kt")

EXTRAS = (
    '    PfLabel("Extras", a)\n'
    "    PfGroup {\n"
    '        PfSwitch(Icons.Rounded.AutoAwesome, "Cat on the mini player", "A smart cat with a tree and a food bowl", LookPrefs.catOn, a) {\n'
    "            LookPrefs.catOn = it\n"
    "            LookPrefs.save()\n"
    "        }\n"
    '        PfSwitch(Icons.Rounded.Palette, "Sky glow on Home", "Sun, sunset or moon that follows the time of day", LookPrefs.skyOn, a) {\n'
    "            LookPrefs.skyOn = it\n"
    "            LookPrefs.save()\n"
    "        }\n"
    "        PfChoice(\n"
    '            Icons.Rounded.Palette, "Try the sky", listOf("Auto", "Morning", "Afternoon", "Evening", "Night"),\n'
    "            when (LookPrefs.previewHour) {\n"
    "                null -> 0\n"
    "                8f -> 1\n"
    "                13f -> 2\n"
    "                18.5f -> 3\n"
    "                else -> 4\n"
    "            },\n"
    '            a, "Preview the glow for each time of day"\n'
    "        ) { LookPrefs.previewHour = listOf<Float?>(null, 8f, 13f, 18.5f, 23f)[it] }\n"
    '        PfSwitch(Icons.Rounded.AutoAwesome, "String lights", "Glowing lights around the logo on the welcome screen", StudioPrefs.lightsOn, a) {\n'
    "            StudioPrefs.lightsOn = it\n"
    "            StudioPrefs.save()\n"
    "        }\n"
    "    }\n"
)
if "LookPrefs.catOn" in t:
    say("Appearance extras (already applied)", True)
else:
    anchor = '    PfLabel("Performance", a)\n'
    if anchor in t:
        t = t.replace(anchor, EXTRAS + anchor, 1)
        say("Appearance extras", True)
    else:
        say("Appearance extras", False)

MUFFLE = (
    '        PfSwitch(Icons.Rounded.Speed, "Muffle between songs", "The end gets muffled and quiet, the next song fades in clear", StudioPrefs.mixOn, a) {\n'
    "            StudioPrefs.mixOn = it\n"
    "            StudioPrefs.save()\n"
    "        }\n"
    "        PfSlider(\n"
    '            Icons.Rounded.Sync, "Muffle length", StudioPrefs.mixSecs.toString() + " s",\n'
    "            StudioPrefs.mixSecs.toFloat(), 5f..15f, a,\n"
    "            { StudioPrefs.mixSecs = it.toInt() }, { StudioPrefs.save() }\n"
    "        )\n"
)
if "Muffle between songs" in t:
    say("Muffle switch (already applied)", True)
else:
    anchor = "            { Opts.crossfade = it.toInt() }, { Opts.save() }\n        )\n"
    if anchor in t:
        t = t.replace(anchor, anchor + MUFFLE, 1)
        say("Muffle switch in Player and audio", True)
    else:
        say("Muffle switch in Player and audio", False)
files["Settings2.kt"] = t

if ok:
    for n, tx in files.items():
        (base / n).write_text(tx)
    print("\nAll good. Now: git add -A && git commit -m v34 && git push")
else:
    print("\nSomething did not match, nothing was changed. Send me this screen.")
    sys.exit(1)
