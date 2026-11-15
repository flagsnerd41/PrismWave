#!/usr/bin/env python3
# fixed zoomed cover in the Immersive player and the messy cover flight when the player opens
# run from anywhere:  python3 ~/PrismWave/patch_fixed_player_cover_flight.py
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

# 1. the cover stage of the Immersive player is nearly square, so the cover is not blown up and cropped
t = must_replace(
    t,
    "        val stageHeight = maxHeight * 0.64f\n",
    "        val stageHeight = minOf(maxWidth * 1.10f, maxHeight * 0.60f)\n",
    "Immersive stage height",
)

# 2. the real cover waits while the flying cover lands on it (no second cover under the flying one)
t = must_replace(
    t,
    "                .fillMaxWidth()\n"
    "                .height(stageHeight)\n"
    "                .then(\n",
    "                .fillMaxWidth()\n"
    "                .height(stageHeight)\n"
    "                .graphicsLayer { alpha = if (FlightState.busy) 0f else 1f }\n"
    "                .then(\n",
    "Immersive cover waits",
)
t = must_replace(
    t,
    "                .shareCover()\n"
    "                .graphicsLayer {\n"
    "                    scaleX = coverScale\n"
    "                    scaleY = coverScale\n"
    "                }\n",
    "                .shareCover()\n"
    "                .graphicsLayer {\n"
    "                    scaleX = coverScale\n"
    "                    scaleY = coverScale\n"
    "                    alpha = if (FlightState.busy) 0f else 1f\n"
    "                }\n",
    "Album cover waits",
)
files["Player.kt"] = t

# ---------------------------------------------------------------- CoverFlight.kt: fly to the same stage
t = load("CoverFlight.kt")
t = must_replace(
    t,
    "            to = sr ?: Rect(0f, 0f, wPx, hPx * 0.64f)\n",
    "            to = sr ?: Rect(0f, 0f, wPx, minOf(wPx * 1.10f, hPx * 0.60f))\n",
    "Flight target",
)
t = must_replace(
    t,
    "            from = sr ?: Rect(0f, 0f, wPx, hPx * 0.64f)\n",
    "            from = sr ?: Rect(0f, 0f, wPx, minOf(wPx * 1.10f, hPx * 0.60f))\n",
    "Flight start when closing",
)
files["CoverFlight.kt"] = t

# ---------------------------------------------------------------- MainActivity.kt: the player appears while the cover lands
t = load("MainActivity.kt")
t = must_replace(
    t,
    "fadeIn(tween(240)) else slideInVertically",
    "fadeIn(tween(300, delayMillis = 140)) else slideInVertically",
    "Player fade in",
)
t = must_replace(
    t,
    "fadeOut(tween(320)) else slideOutVertically",
    "fadeOut(tween(240)) else slideOutVertically",
    "Player fade out",
)
files["MainActivity.kt"] = t

# ---------------------------------------------------------------- Settings2.kt, Extras.kt
t = load("Settings2.kt")
marker = "private fun PfChangelogPage(s: AppState) {\n    val a = s.accent\n"
t = must_replace(
    t,
    marker,
    marker +
    '    PfChange("Fixed zoomed cover and messy open animation in the player", a, listOf(\n'
    '        "The Immersive player cover is nearly square now, so it is no longer blown up and cropped",\n'
    '        "Opening the player: the cover flies to its place alone, then the player fades in around it"\n'
    '    ))\n',
    "Changelog",
)
files["Settings2.kt"] = t

t = load("Extras.kt")
t, n = re.subn(r'const val ID = "[^"]*"', 'const val ID = "fixed-player-cover-flight"', t, count=1)
if n != 1:
    print("FAILED: WhatsNew id")
    sys.exit(1)
files["Extras.kt"] = t

for name, text in files.items():
    with open(os.path.join(ROOT, name), "w", encoding="utf-8") as f:
        f.write(text)
    print("patched " + name)
print("done")
