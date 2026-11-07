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

# ---------------- State.kt
sp = os.path.join(app, "State.kt")
t = read(sp)
if "AutoMix" not in t:
    pairs = [
        ("    private fun onAutoAdvance(sg: Song) {\n",
         "    private fun onAutoAdvance(sg: Song) {\n"
         "        val prevLead = if (AutoMix.on && fading) AutoMix.leadFor(current?.id ?: \"\") else null\n"),
        ("            val ms = StudioPrefs.mixSecs * 1000L\n",
         "            val ms = ((prevLead ?: StudioPrefs.mixSecs.toFloat()) * 1000f).toLong()\n"),
        ("        } else if (Opts.crossfade > 0) {\n            fade(0f, 1f, Opts.crossfade * 1000L)\n",
         "        } else if (Opts.crossfade > 0 || prevLead != null) {\n"
         "            fade(0f, 1f, ((prevLead ?: Opts.crossfade.toFloat()) * 1000f).toLong())\n"),
        ("                val nM = StudioPrefs.mixSecs.toFloat()\n",
         "                val nM = (if (AutoMix.on) AutoMix.leadFor(current?.id ?: \"\") else null) ?: StudioPrefs.mixSecs.toFloat()\n"),
        ("            if (Opts.crossfade > 0 && dur > 0 && !fading && !repeat) {\n",
         "            if ((Opts.crossfade > 0 || AutoMix.smart(current?.id)) && dur > 0 && !fading && !repeat) {\n"),
        ("                val cf = Opts.crossfade.toFloat()\n",
         "                val cf = (if (AutoMix.on) AutoMix.leadFor(current?.id ?: \"\") else null) ?: Opts.crossfade.toFloat()\n"),
        ("                    if (SongCache.fetch(ctx, sg.id, url) && queuedId == sg.id) refreshQueued()\n",
         "                    if (SongCache.fetch(ctx, sg.id, url)) {\n"
         "                        AutoMix.analyze(ctx, sg.id)\n"
         "                        if (queuedId == sg.id) refreshQueued()\n"
         "                    }\n"),
        ("                cacheCurrent(song, url)\n",
         "                cacheCurrent(song, url)\n"
         "                scope.launch { AutoMix.analyze(ctx, song.id) }\n"),
        ("                SongCache.fetch(ctx, song.id, url)\n",
         "                SongCache.fetch(ctx, song.id, url)\n"
         "                AutoMix.analyze(ctx, song.id)\n"),
    ]
    for old, new in pairs:
        if t.count(old) != 1:
            fail("State.kt: could not find (" + str(t.count(old)) + " matches): " + old.strip()[:70])
        t = t.replace(old, new)
    write(sp, t)
    print("State.kt patched: AutoMix hooks")
else:
    print("State.kt already patched")

# ---------------- Settings2.kt: Transitions page + changelog
sp = os.path.join(app, "Settings2.kt")
t = read(sp)
page = read(os.path.join(here, "transitions_page.txt"))
i = t.find("@Composable\nprivate fun PfTransitionsPage(")
if i < 0:
    fail("Settings2.kt: Transitions page not found, apply the previous zip first")
t = t[:i].rstrip() + "\n\n" + page
if "Added AutoMix" not in t:
    key = '    PfChange("Added cover blur-out'
    entry = (
        '    PfChange("Added AutoMix smart mix point", a, listOf(\n'
        '        "AutoMix listens to the end of a cached song and starts the transition where the outro begins",\n'
        '        "Uses your crossfade or blur-out style, with a smart length for every song"\n'
        '    ))\n'
    )
    if t.count(key) == 1:
        t = t.replace(key, entry + key, 1)
write(sp, t)
print("Settings2.kt: AutoMix switch added")
print("All done. Now: git add -A && git commit && git push")
