import os, sys
d = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app")

def patch(fname, edits):
    path = os.path.join(d, fname)
    t = open(path).read()
    for old, new in edits:
        if new in t:
            print(fname, "already patched, skipping one edit")
            continue
        if t.count(old) != 1:
            print("FAILED", fname, "anchor not found:", old[:50]); sys.exit(1)
        t = t.replace(old, new)
    open(path, "w").write(t)
    print("patched", fname)

patch("Account.kt", [
("    private fun parseSong(r: JSONObject): Song? {",
'''    private val durRegex = Regex("^\\\\d{1,2}(:\\\\d{2}){1,2}$")

    private fun findDurationSecs(node: Any?): Int {
        when (node) {
            is JSONObject -> {
                val keys = node.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = node.opt(k)
                    if (v is String && (k == "text" || k == "simpleText") && durRegex.matches(v.trim())) {
                        return seconds(v)
                    }
                    val r = findDurationSecs(v)
                    if (r > 0) return r
                }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                val r = findDurationSecs(node.opt(i))
                if (r > 0) return r
            }
        }
        return 0
    }

    private fun parseSong(r: JSONObject): Song? {'''),
("        return Song(url, title, artist, seconds(firstRunText(dur)), url, thumb)",
'''        var secs = seconds(firstRunText(dur))
        if (secs <= 0) secs = findDurationSecs(r)
        return Song(url, title, artist, secs, url, thumb)'''),
])

patch("State.kt", [
("    fun tick() {\n",
'''    // Songs imported with a missing length get the real one from the player once it is ready.
    private fun fixDuration(real: Int) {
        val cur = current ?: return
        if (real < 2 || kotlin.math.abs(cur.seconds - real) <= 2) return
        val fixed = cur.copy(seconds = real)
        current = fixed
        queue = queue.map { if (it.id == cur.id) fixed else it }
        known[cur.id] = fixed
        val li = liked.indexOfFirst { it.id == cur.id }
        if (li >= 0) liked[li] = fixed
        val hi = history.indexOfFirst { it.id == cur.id }
        if (hi >= 0) history[hi] = fixed
        for (i in playlists.indices) {
            val p = playlists[i]
            if (p.songs.any { it.id == cur.id }) {
                playlists[i] = p.copy(songs = p.songs.map { if (it.id == cur.id) fixed else it })
            }
        }
        savePlaylists()
        saveAll()
    }

    fun tick() {
'''),
("            position = c.currentPosition / 1000f\n",
'''            position = c.currentPosition / 1000f
            val dur = c.duration
            if (dur > 0) fixDuration((dur / 1000).toInt())
'''),
])
print("done")
