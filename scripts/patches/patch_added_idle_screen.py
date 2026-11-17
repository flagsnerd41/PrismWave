#!/usr/bin/env python3
# added idle screen (always on display) and a smoother full player -> half player animation
# run from anywhere:  python3 ~/PrismWave/patch_added_idle_screen.py
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

# ---------------------------------------------------------------- Player.kt
t = load("Player.kt")

# 1. every touch on the full player restarts the idle clock
t = must_replace(
    t,
    "    Box(\n"
    "        Modifier\n"
    "            .fillMaxSize()\n"
    "            .graphicsLayer {\n"
    "                translationY = if (SharedCover.rect != null) 0f else lyShift * size.height * 0.18f",
    "    Box(\n"
    "        Modifier\n"
    "            .fillMaxSize()\n"
    "            .aodTouch()\n"
    "            .graphicsLayer {\n"
    "                translationY = if (SharedCover.rect != null) 0f else lyShift * size.height * 0.18f",
    "touch watcher",
)

# 2. the step from the player to the half view: slow and smooth when the idle timer does it
t = must_replace(
    t,
    "    LaunchedEffect(PlayerStage.stage) {\n"
    "        halfP.animateTo(PlayerStage.stage.toFloat(), spring(dampingRatio = 0.9f, stiffness = 150f))\n"
    "    }",
    "    LaunchedEffect(PlayerStage.stage) {\n"
    "        if (Aod.auto) {\n"
    "            halfP.animateTo(PlayerStage.stage.toFloat(), tween(1100, easing = FastOutSlowInEasing))\n"
    "        } else {\n"
    "            halfP.animateTo(PlayerStage.stage.toFloat(), spring(dampingRatio = 0.9f, stiffness = 150f))\n"
    "        }\n"
    "    }",
    "half animation",
)

# 3. the idle screen sits on top of everything in the full player
t = must_replace(
    t,
    "        if (halfVisible) {\n"
    "            LyricsStage(s, song) { halfP.value }\n"
    "        }",
    "        if (halfVisible) {\n"
    "            LyricsStage(s, song) { halfP.value }\n"
    "        }\n"
    "        AodHost(s, song, menuDialog || addDialog || queueDialog || sleepDialog)",
    "idle host",
)
files["Player.kt"] = t

# ---------------------------------------------------------------- Settings2.kt
t = load("Settings2.kt")

t = must_replace(
    t,
    '    PfLabel("Video (experiment)", a)',
    '    Aod.init(LocalContext.current.applicationContext)\n'
    '    PfLabel("Idle screen", a)\n'
    '    PfGroup {\n'
    '        PfSwitch(\n'
    '            Icons.Rounded.Speed, "Always on display",\n'
    '            "Idle on the player: half view, then a black screen with lyrics. The screen stays on while music plays on the player",\n'
    '            Aod.enabled, a\n'
    '        ) {\n'
    '            Aod.enabled = it\n'
    '            Aod.save()\n'
    '        }\n'
    '        PfSlider(\n'
    '            Icons.Rounded.History, "Half view after", Aod.halfMin.toString() + " min",\n'
    '            Aod.halfMin.toFloat(), 1f..5f, a,\n'
    '            { Aod.halfMin = it.toInt().coerceIn(1, 5) }, { Aod.save() }\n'
    '        )\n'
    '        PfSlider(\n'
    '            Icons.Rounded.History, "Black screen after another", Aod.blackMin.toString() + " min",\n'
    '            Aod.blackMin.toFloat(), 1f..5f, a,\n'
    '            { Aod.blackMin = it.toInt().coerceIn(1, 5) }, { Aod.save() }\n'
    '        )\n'
    '    }\n'
    '    PfLabel("Video (experiment)", a)',
    "Idle settings",
)

marker = "private fun PfChangelogPage(s: AppState) {\n    val a = s.accent\n"
t = must_replace(
    t,
    marker,
    marker +
    '    PfChange("Added idle screen", a, listOf(\n'
    '        "Idle on the full player: after a minute it goes to the half view, after another minute to a black screen",\n'
    '        "The black screen has a small cover, the controls, and the lyrics at the bottom with the app icon",\n'
    '        "Touch the empty black screen to come back. Timers are in Settings, Player",\n'
    '        "The full player to half player step is slower and smoother when the idle timer does it"\n'
    '    ))\n',
    "Changelog",
)
files["Settings2.kt"] = t

# ---------------------------------------------------------------- Extras.kt (red dot on What's new)
t = load("Extras.kt")
t, n = re.subn(r'const val ID = "[^"]*"', 'const val ID = "added-idle-screen"', t, count=1)
if n != 1:
    print("FAILED: WhatsNew id")
    sys.exit(1)
files["Extras.kt"] = t

for name, text in files.items():
    with open(os.path.join(ROOT, name), "w", encoding="utf-8") as f:
        f.write(text)
    print("patched " + name)
print("done")
