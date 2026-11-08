import os, sys

here = os.path.dirname(os.path.abspath(__file__))
app = os.path.join(here, "app", "src", "main", "java", "com", "prismwave", "app")

def fail(msg):
    print("PATCH FAILED:", msg)
    sys.exit(1)

def read(p):
    return open(p, encoding="utf-8").read()

def write(p, t):
    open(p, "w", encoding="utf-8").write(t)

def patch(path, pairs, marker):
    t = read(path)
    if marker in t:
        print(os.path.basename(path), "already patched")
        return
    for old, new in pairs:
        if t.count(old) != 1:
            fail(os.path.basename(path) + ": found " + str(t.count(old)) + " times: " + old.strip()[:70])
        t = t.replace(old, new)
    write(path, t)
    print(os.path.basename(path), "patched")

# ---- State.kt: fades run inside the audio, with a soft floor so the sound never drops to silence
patch(os.path.join(app, "State.kt"), [
    ("fade(c.volume, 0f, (left * 1000).toLong() - 150)",
     "AudioShape.rampGain(1f, 0.35f, (left * 1000).toLong() - 150)"),
    ("fade(0f, 1f, ((prevLead ?: Opts.crossfade.toFloat()) * 1000f).toLong())",
     "AudioShape.rampGain(0.35f, 1f, ((prevLead ?: Opts.crossfade.toFloat()) * 1000f).toLong())"),
    ("    fun seekTo(sec: Float) {\n        position = sec\n",
     "    fun seekTo(sec: Float) {\n        position = sec\n"
     "        if (fading) {\n"
     "            fading = false\n"
     "            fadeJob?.cancel()\n"
     "            controller?.volume = 1f\n"
     "            MuffleMix.clear()\n"
     "        }\n"),
    ("            controller?.volume = 1f\n            controller?.seekTo(0)\n",
     "            controller?.volume = 1f\n            AudioShape.reset()\n            controller?.seekTo(0)\n"),
], "AudioShape")

# ---- Studio.kt: the audio blur-out now runs inside the audio (equalizer path stays for the Sound Studio)
patch(os.path.join(app, "Studio.kt"), [
    ("    fun ramp(a: Float, b: Float, ms: Long) {\n        cancel()\n",
     "    fun ramp(a: Float, b: Float, ms: Long) {\n        AudioShape.rampMuffle(a, b, ms)\n    }\n\n"
     "    private fun rampEq(a: Float, b: Float, ms: Long) {\n        cancel()\n"),
    ("    fun clear() {\n        cancel()\n        outgoing = false\n",
     "    fun clear() {\n        cancel()\n        outgoing = false\n        AudioShape.reset()\n"),
], "AudioShape")

# ---- Settings2.kt: Transitions page
sp = os.path.join(app, "Settings2.kt")
t = read(sp)
page = read(os.path.join(here, "transitions_page.txt"))
i = t.find("@Composable\nprivate fun PfTransitionsPage(")
if i < 0:
    fail("Settings2.kt: Transitions page not found")
t = t[:i].rstrip() + "\n\n" + page
write(sp, t)
print("Settings2.kt: Transitions page updated")
print("All done. Now: git add -A && git commit && git push")
