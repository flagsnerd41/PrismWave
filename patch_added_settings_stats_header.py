import os, re, sys

base = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
snippet = open(os.path.expanduser("~/PrismWave/settings_snippet.kt"), encoding="utf-8").read()


def load(name):
    return open(base + name, encoding="utf-8").read()


def save(name, text):
    open(base + name, "w", encoding="utf-8").write(text)


def rep(text, old, new, what):
    n = text.count(old)
    if n != 1:
        print("FAILED (%s): found %d times" % (what, n))
        sys.exit(1)
    return text.replace(old, new)


# ---------------------------------------------------------------- Settings2.kt
s = load("Settings2.kt")
# the About hero from the last commit used graphicsLayer without importing it (this broke the build)
if "import androidx.compose.ui.graphics.graphicsLayer\n" not in s:
    s = s.replace("import androidx.compose.ui.graphics.Color\n",
                  "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.graphicsLayer\n", 1)
    save("Settings2.kt", s)
    print("fixed missing graphicsLayer import")
if "PfStatsPage2" in s:
    print("Settings2.kt already patched")
else:
    s = rep(s, "import androidx.compose.material.icons.rounded.Speed\n",
            "import androidx.compose.material.icons.rounded.Speed\nimport androidx.compose.material.icons.rounded.Subject\n", "import")
    s = rep(s, "// ------------------------------------------------------------ main list (Material 3)",
            snippet + "// ------------------------------------------------------------ main list (Material 3)", "snippet")
    s = rep(s, '        "stats" to "Listening Stats",\n',
            '        "stats" to "Listening Stats",\n        "lyrics" to "Lyrics",\n', "titles")
    start_marker = '                    PfHero(mainScroll) { page = "about" }\n'
    end_marker = "                } else {\n                    when (pg) {"
    a = s.find(start_marker)
    b = s.find(end_marker)
    if a < 0 or b < 0 or b < a:
        print("FAILED (main page block)")
        sys.exit(1)
    new_block = '''                    PfProfileCard(s) { page = "account" }
                    PfLabel("Essentials", acc)
                    PfEssentials(s) { page = it }
                    PfLabel("Player & Content", acc)
                    PfMainGroup(
                        listOf(
                            PfItem("transitions", "Transitions", "Crossfade, blur-out, 8D and cover flip", Icons.Rounded.Sync),
                            PfItem("content", "Content", "Choose what shows on Home", Icons.Rounded.Home),
                            PfItem("behavior", "Behavior", "Haptics and tap feel", Icons.Rounded.TouchApp),
                            PfItem("integration", "Integration", "Discord, Last.fm, Libre.fm and ListenBrainz", Icons.Rounded.AutoAwesome),
                            PfItem("storage", "Storage", "Cached songs and size limit", Icons.Rounded.Download)
                        ),
                        acc, 0
                    ) { page = it }
                    PfMainGroup(
                        listOf(
                            PfItem("backup", "Backup and restore", "Save or bring back your library", Icons.Rounded.Folder),
                            PfItem("changelog", "What's new", "Changes in each version", Icons.Rounded.History),
                            PfItem("about", "About PrismWave", "Version 1.5 and updates", Icons.Rounded.Info)
                        ),
                        acc, 5
                    ) { page = it }
'''
    s = s[:a] + new_block + s[b:]
    s = rep(s, '                        "stats" -> PfStatsPage(s)\n',
            '                        "stats" -> PfStatsPage2(s)\n                        "lyrics" -> PfLyricsPage(s)\n', "when")
    s = rep(s, "private fun PfChangelogPage(s: AppState) {\n    val a = s.accent\n",
            'private fun PfChangelogPage(s: AppState) {\n    val a = s.accent\n'
            '    PfChange("Added new Settings, Listening Stats ranges, header menu and morphing icons", a, listOf(\n'
            '        "Settings: profile card with Manage account, Essentials tiles with morphing shape icons, Player and Content list",\n'
            '        "Listening Stats: time ranges (1 week to All time), total time, plays, unique songs and artists, top songs and artists",\n'
            '        "Lyrics settings: text size and centered lines",\n'
            '        "Home header: one round menu button with profile, New releases, News From Developers, History, Listening Stats and Settings"\n'
            '    ))\n', "changelog")
    save("Settings2.kt", s)
    print("patched Settings2.kt")

# ---------------------------------------------------------------- Screens.kt (header)
s = load("Screens.kt")
if "HeaderMenuButton" in s:
    print("Screens.kt already patched")
else:
    old = '''        RoundBtn(Icons.Rounded.History, 0.9f, {
            s.openPlaylist = null
            s.libraryOpen = "History"
            s.screen = Screen.Library
        }, 46.dp)
        Spacer(Modifier.width(8.dp))
        RoundBtn(Icons.Rounded.BarChart, 0.9f, { stats = true }, 46.dp)
        Spacer(Modifier.width(8.dp))
        WhatsNewButton(s)
        Spacer(Modifier.width(8.dp))
        RoundBtn(Icons.Rounded.Settings, 0.9f, { s.screen = Screen.Settings }, 46.dp)
'''
    s = rep(s, old, "        HeaderMenuButton(s)\n", "header buttons")
    s = rep(s, "        ) { AppLogo(42.dp) }\n",
            "        ) { MorphBadge(50.dp, s.accent.copy(alpha = 0.40f)) { AppLogo(34.dp) } }\n", "header logo")
    save("Screens.kt", s)
    print("patched Screens.kt")

# ---------------------------------------------------------------- Extras.kt (red dot for the new What's new)
s = load("Extras.kt")
s2 = re.sub(r'const val ID = "[^"]*"', 'const val ID = "added-settings-stats-header"', s, count=1)
if s2 == s:
    print("Extras.kt: ID unchanged")
save("Extras.kt", s2)

# ---------------------------------------------------------------- State.kt (stats data)
s = load("State.kt")
if "statSeconds" in s:
    print("State.kt already patched")
else:
    s = rep(s, "    private var warmedFor: String? = null\n",
            '''    private var warmedFor: String? = null
    // listening time per day (yyyyMMdd to seconds) and every play with its time, for the stats ranges
    val dayLog = HashMap<String, Int>()
    private val playLog = ArrayList<Pair<Long, String>>()
''', "fields")
    s = rep(s, "        connect()\n        watchHeadphones()\n",
            "        loadStats()\n        connect()\n        watchHeadphones()\n", "init")
    s = rep(s, "            val ls = listenSeconds\n            withContext(Dispatchers.Default) {\n",
            "            val ls = listenSeconds\n            val dl = dayLogJson()\n            val pl = playLogJson()\n            withContext(Dispatchers.Default) {\n", "save snapshots")
    s = rep(s, '                    .putInt("listenSeconds", ls)\n                    .apply()\n',
            '                    .putInt("listenSeconds", ls)\n                    .putString("dayLog", dl)\n                    .putString("playLog", pl)\n                    .apply()\n', "save put")
    s = rep(s, "            listenSeconds++\n            ticks++\n",
            "            listenSeconds++\n            val dk = dayKey(System.currentTimeMillis())\n            dayLog[dk] = (dayLog[dk] ?: 0) + 1\n            ticks++\n", "tick")
    s = rep(s, "        known[song.id] = song\n        counts[song.id] = (counts[song.id] ?: 0) + 1\n",
            "        known[song.id] = song\n        counts[song.id] = (counts[song.id] ?: 0) + 1\n        logPlay(song.id)\n", "play log")
    s = rep(s, "        known[sg.id] = sg\n        counts[sg.id] = (counts[sg.id] ?: 0) + 1\n",
            "        known[sg.id] = sg\n        counts[sg.id] = (counts[sg.id] ?: 0) + 1\n        logPlay(sg.id)\n", "auto play log")
    funcs = '''    // ---------- listening stats ----------
    private fun dayKey(ms: Long): String =
        java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date(ms))

    private fun loadStats() {
        try {
            val o = JSONObject(prefs.getString("dayLog", "{}") ?: "{}")
            o.keys().forEach { dayLog[it] = o.getInt(it) }
        } catch (e: Exception) {
        }
        try {
            val a = JSONArray(prefs.getString("playLog", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val r = a.getJSONArray(i)
                playLog.add(Pair(r.getLong(0), r.getString(1)))
            }
        } catch (e: Exception) {
        }
    }

    private fun dayLogJson(): String {
        val cut = dayKey(System.currentTimeMillis() - 400L * 86400000L)
        dayLog.keys.removeAll { it < cut }
        return JSONObject(HashMap(dayLog) as Map<*, *>).toString()
    }

    private fun playLogJson(): String {
        val a = JSONArray()
        for (p in playLog) a.put(JSONArray().put(p.first).put(p.second))
        return a.toString()
    }

    private fun logPlay(id: String) {
        playLog.add(Pair(System.currentTimeMillis(), id))
        while (playLog.size > 3000) playLog.removeAt(0)
    }

    // days = 0 means all time
    fun statSeconds(days: Int): Int {
        if (days == 0) return listenSeconds
        var sum = 0
        val now = System.currentTimeMillis()
        for (i in 0 until days) sum += dayLog[dayKey(now - i * 86400000L)] ?: 0
        return sum
    }

    fun statCounts(days: Int): Map<String, Int> {
        if (days == 0) return HashMap(counts)
        val cut = System.currentTimeMillis() - days * 86400000L
        val m = HashMap<String, Int>()
        for (p in playLog) if (p.first >= cut) m[p.second] = (m[p.second] ?: 0) + 1
        return m
    }

    fun songOf(id: String): Song? = known[id]

'''
    s = rep(s, "    fun topArtists(): List<Pair<String, Int>> =", funcs + "    fun topArtists(): List<Pair<String, Int>> =", "functions")
    save("State.kt", s)
    print("patched State.kt")
print("done")
