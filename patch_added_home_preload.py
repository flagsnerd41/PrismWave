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

def replace_once(t, old, new, label):
    n = t.count(old)
    if n != 1:
        fail(label + ": found " + str(n) + " times: " + old.strip()[:70])
    return t.replace(old, new)

# ---------------- State.kt: no fade at song start, covers preloaded 30 s before the end
sp = os.path.join(app, "State.kt")
t = read(sp)
if "warmCovers" not in t:
    t = replace_once(
        t,
        "                val soft = (Opts.fade || Opts.crossfade > 0) && !(autoAdvance && Opts.crossfade == 0)\n",
        "                val soft = false\n",
        "State.kt soft")
    t = replace_once(
        t,
        "    var radio = false\n",
        "    var radio = false\n"
        "    var coverPool: List<Song> = emptyList()\n"
        "    private var warmedFor: String? = null\n",
        "State.kt fields")
    t = replace_once(
        t,
        "    fun tick() {\n",
        "    // 30 seconds before a song ends: load the next cover and a few random ones\n"
        "    private fun warmCovers() {\n"
        "        val cur = current ?: return\n"
        "        val next = upcoming().firstOrNull()\n"
        "        val pool = (queue + history.toList() + liked.toList())\n"
        "            .filter { it.id != cur.id && it.id != next?.id && it.thumb != null }\n"
        "            .distinctBy { it.id }\n"
        "            .shuffled()\n"
        "            .take(12)\n"
        "        coverPool = pool\n"
        "        val q = if (playerStyle == 1) 1200 else 1000\n"
        "        CoverPreload.warm(ctx, listOfNotNull(next) + pool, q)\n"
        "    }\n\n"
        "    fun tick() {\n",
        "State.kt tick")
    t = replace_once(
        t,
        "            listenSeconds++\n            ticks++\n",
        "            if (dur > 0 && !repeat) {\n"
        "                val left30 = dur / 1000f - position\n"
        "                val cid = current?.id\n"
        "                if (cid != null && cid != warmedFor && left30 > 0f && left30 <= 30f) {\n"
        "                    warmedFor = cid\n"
        "                    warmCovers()\n"
        "                }\n"
        "            }\n"
        "            listenSeconds++\n            ticks++\n",
        "State.kt tick body")
    write(sp, t)
    print("State.kt patched")
else:
    print("State.kt already patched")

# ---------------- Screens.kt: Home layout like the recording
sp = os.path.join(app, "Screens.kt")
t = read(sp)
if "QuickPickCarousel" not in t:
    t = replace_once(
        t,
        "@Composable\nfun HomeScreen(s: AppState) {\n",
        "@Composable\nprivate fun HomeScreenV22(s: AppState) {\n",
        "Screens.kt rename")
    marker = "// ------------------------------------------------------------ home header picture\n"
    block = read(os.path.join(here, "home_block.txt"))
    t = replace_once(t, marker, block + marker, "Screens.kt insert")
    imports = [
        "androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior",
        "androidx.compose.foundation.lazy.LazyListState",
        "androidx.compose.foundation.lazy.rememberLazyListState",
        "androidx.compose.material.icons.rounded.MoreHoriz",
    ]
    add = ""
    for imp in imports:
        if ("import " + imp + "\n") not in t:
            add += "import " + imp + "\n"
    t = replace_once(t, "package com.prismwave.app\n", "package com.prismwave.app\n\n" + add, "Screens.kt imports")
    write(sp, t)
    print("Screens.kt patched: new Home layout")
else:
    print("Screens.kt already patched")

# ---------------- Settings2.kt: changelog
sp = os.path.join(app, "Settings2.kt")
t = read(sp)
key = '    PfChange("Added AutoMix smart mix point"'
if "Added Home layout" not in t and t.count(key) == 1:
    entry = (
        '    PfChange("Added Home layout, cover preload and removed start fade", a, listOf(\n'
        '        "Home: big Quick picks carousel, mood chips and From the community, like ArchiveTune",\n'
        '        "Covers load 30 seconds before the song ends, so the cover shuffle is instant",\n'
        '        "Songs start at full volume, no fade-in at the start"\n'
        '    ))\n'
    )
    t = t.replace(key, entry + key, 1)
    write(sp, t)
    print("Settings2.kt: changelog added")
print("All done. Now: git add -A && git commit && git push")
