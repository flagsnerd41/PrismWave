#!/usr/bin/env python3
# added option to hide the volume slider (player and menu)
# run from anywhere:  python3 ~/PrismWave/patch_added_volume_slider_option.py
import os
import re
import sys

ROOT = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app")


def load(name):
    with open(os.path.join(ROOT, name), encoding="utf-8") as f:
        return f.read()


def must_replace(text, old, new, label):
    n = text.count(old)
    if n != 1:
        print("FAILED: " + label + " (found " + str(n) + " times, needed 1)")
        sys.exit(1)
    return text.replace(old, new, 1)


files = {}

t = load("Player.kt")
t = must_replace(
    t,
    "private fun VolumeRow() {\n    val ctx = LocalContext.current\n",
    "private fun VolumeRow() {\n    val ctx = LocalContext.current\n    VolPrefs.init(ctx.applicationContext)\n    if (!VolPrefs.show) return\n",
    "Volume row",
)
t = must_replace(
    t,
    "    var tempoOpen by remember { mutableStateOf(false) }\n    LaunchedEffect(Unit) { visible = true }\n",
    "    var tempoOpen by remember { mutableStateOf(false) }\n    VolPrefs.init(ctx.applicationContext)\n    LaunchedEffect(Unit) { visible = true }\n",
    "Menu init",
)
t = must_replace(
    t,
    "            PopItem(1, o) { VolumeCard(s.accent) }\n",
    "            if (VolPrefs.showMenu) PopItem(1, o) { VolumeCard(s.accent) }\n",
    "Menu volume card",
)
files["Player.kt"] = t

t = load("Settings2.kt")
t = must_replace(
    t,
    "import androidx.compose.ui.platform.LocalContext\n",
    "import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.material.icons.rounded.VolumeUp\n",
    "VolumeUp import",
)
t = must_replace(
    t,
    '    PfLabel("Idle screen", a)\n',
    '    VolPrefs.init(LocalContext.current.applicationContext)\n'
    '    PfLabel("Volume", a)\n'
    '    PfGroup {\n'
    '        PfSwitch(\n'
    '            Icons.Rounded.VolumeUp, "Volume slider in the player",\n'
    '            "Show the volume slider under the controls", VolPrefs.show, a\n'
    '        ) {\n'
    '            VolPrefs.show = it\n'
    '            VolPrefs.save()\n'
    '        }\n'
    '        PfSwitch(\n'
    '            Icons.Rounded.VolumeUp, "Volume in the menu",\n'
    '            "Show the volume card in the three dots menu", VolPrefs.showMenu, a\n'
    '        ) {\n'
    '            VolPrefs.showMenu = it\n'
    '            VolPrefs.save()\n'
    '        }\n'
    '    }\n'
    '    PfLabel("Idle screen", a)\n',
    "Volume settings",
)
marker = "private fun PfChangelogPage(s: AppState) {\n    val a = s.accent\n"
t = must_replace(
    t,
    marker,
    marker +
    '    PfChange("Added option to hide the volume slider", a, listOf(\n'
    '        "Settings, Player, Volume: turn the slider off in the player and in the three dots menu"\n'
    '    ))\n',
    "Changelog",
)
files["Settings2.kt"] = t

t = load("Extras.kt")
t, n = re.subn(r'const val ID = "[^"]*"', 'const val ID = "added-volume-slider-option"', t, count=1)
if n != 1:
    print("FAILED: WhatsNew id")
    sys.exit(1)
files["Extras.kt"] = t

for name, text in files.items():
    with open(os.path.join(ROOT, name), "w", encoding="utf-8") as f:
        f.write(text)
    print("patched " + name)
print("done")
