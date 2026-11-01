import pathlib
import sys

p = pathlib.Path.home() / "PrismWave/app/src/main/java/com/prismwave/app/State.kt"
s = p.read_text()
ok = True


def step(name, marker, old, new):
    global s, ok
    if marker in s:
        print("OK    " + name + " (already applied)")
        return
    if old not in s:
        print("FAIL  " + name)
        ok = False
        return
    s = s.replace(old, new, 1)
    print("OK    " + name)


# 1) start of the muffle in the last seconds (and undo it if you seek back)
step(
    "muffle in tick",
    "MuffleMix.outgoing = true",
    "            if (!fading && fadeJob?.isActive != true && c.volume < 0.99f) c.volume = 1f\n",
    "            if (!fading && fadeJob?.isActive != true && c.volume < 0.99f) c.volume = 1f\n"
    "            if (StudioPrefs.mixOn && dur > 0 && !repeat) {\n"
    "                val leftM = dur / 1000f - position\n"
    "                val nM = StudioPrefs.mixSecs.toFloat()\n"
    "                if (!fading && dur / 1000f > nM * 3 && leftM >= 0.5f && leftM <= nM) {\n"
    "                    fading = true\n"
    "                    MuffleMix.outgoing = true\n"
    "                    MuffleMix.ramp(0f, StudioPrefs.mixStrength, (leftM * 1000).toLong() - 150)\n"
    "                    fade(c.volume, 0.3f, (leftM * 1000).toLong() - 150)\n"
    "                } else if (MuffleMix.outgoing && leftM > nM + 1f) {\n"
    "                    MuffleMix.clear()\n"
    "                    fading = false\n"
    "                    fadeJob?.cancel()\n"
    "                    c.volume = 1f\n"
    "                }\n"
    "            }\n",
)

# 2) the next song comes in clear and loud slowly
step(
    "muffle off on next song",
    "if (MuffleMix.outgoing) {",
    "        if (Opts.crossfade > 0) {\n            fade(0f, 1f, Opts.crossfade * 1000L)\n",
    "        if (MuffleMix.outgoing) {\n"
    "            MuffleMix.outgoing = false\n"
    "            val ms = StudioPrefs.mixSecs * 1000L\n"
    "            MuffleMix.ramp(StudioPrefs.mixStrength, 0f, ms)\n"
    "            fade(0.3f, 1f, ms)\n"
    "        } else if (Opts.crossfade > 0) {\n            fade(0f, 1f, Opts.crossfade * 1000L)\n",
)

# 3) skipping by hand clears the muffle
step(
    "clear on manual play",
    "MuffleMix.clear()\n        fading = false\n        queuedId = null",
    "        fading = false\n        queuedId = null\n        pausedByUnplug = false\n",
    "        MuffleMix.clear()\n        fading = false\n        queuedId = null\n        pausedByUnplug = false\n",
)

if ok:
    p.write_text(s)
    print("\nAll good. Now: git add -A && git commit -m v30 && git push")
else:
    print("\nSomething did not match, nothing was changed. Send me this screen.")
    sys.exit(1)
