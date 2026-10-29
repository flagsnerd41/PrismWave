import os, sys

ROOT = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
edits = {}

def add(fname, old, new):
    edits.setdefault(fname, []).append((old, new))

# ---------------- Account.kt: read the real YouTube Music Home of your account
add("Account.kt",
    'class RemotePlaylist(val browseId: String, val name: String)',
    'class RemotePlaylist(val browseId: String, val name: String)\n\nclass YtShelf(val title: String, val songs: List<Song>)')

add("Account.kt",
    '    suspend fun profilePhoto(cookie: String): String? = withContext(Dispatchers.IO) {',
    '''    private fun cleanArtist(raw: String): String {
        val parts = raw.split(" \\u2022 ").map { it.trim() }.filter {
            it.isNotEmpty() && it != "Song" && it != "Video" && it != "Single" && it != "Album" && it != "EP" &&
                !it.endsWith(" plays") && !it.endsWith(" views")
        }
        return parts.firstOrNull() ?: "Unknown"
    }

    private fun parseTwoRowSong(r: JSONObject): Song? {
        val vid = r.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId").orEmpty()
        if (vid.isEmpty()) return null
        val title = firstRunText(r.optJSONObject("title"))
        if (title.isEmpty()) return null
        val thumb = lastThumb(
            r.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        )
        val url = "https://www.youtube.com/watch?v=" + vid
        return Song(url, title, cleanArtist(runsText(r.optJSONObject("subtitle"))), 0, url, thumb)
    }

    private fun collectShelves(json: JSONObject, out: MutableList<YtShelf>) {
        walk(json) { key, v ->
            if (key == "musicCarouselShelfRenderer") {
                val title = firstRunText(
                    v.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                        ?.optJSONObject("title")
                )
                val items = v.optJSONArray("contents")
                if (title.isNotEmpty() && items != null) {
                    val songs = ArrayList<Song>()
                    for (i in 0 until items.length()) {
                        val c = items.optJSONObject(i) ?: continue
                        val row = c.optJSONObject("musicResponsiveListItemRenderer")
                        if (row != null) {
                            parseSong(row)?.let { songs.add(it.copy(artist = cleanArtist(it.artist))) }
                        }
                        val two = c.optJSONObject("musicTwoRowItemRenderer")
                        if (two != null) parseTwoRowSong(two)?.let { songs.add(it) }
                    }
                    val uniq = songs.distinctBy { it.id }
                    if (uniq.size >= 3) out.add(YtShelf(title, uniq))
                }
            }
        }
    }

    /** The real YouTube Music Home of the signed-in account (Quick picks, Listen again, ...). */
    suspend fun home(cookie: String): List<YtShelf> = withContext(Dispatchers.IO) {
        val out = ArrayList<YtShelf>()
        var json = browse(cookie, "FEmusic_home", null)
        collectShelves(json, out)
        var pages = 0
        while (pages < 3) {
            val token = findContinuation(json) ?: break
            json = browse(cookie, null, token)
            collectShelves(json, out)
            pages++
        }
        out.distinctBy { it.title }
    }

    suspend fun profilePhoto(cookie: String): String? = withContext(Dispatchers.IO) {''')

# ---------------- Engine.kt: related songs share the cached fetch
add("Engine.kt",
    '        init()\n        org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(ServiceList.YouTube, url)\n            .relatedItems',
    '        infoFor(url)\n            .relatedItems')

# ---------------- State.kt: load the YouTube Music Home, and taste-based picks as a backup
add("State.kt",
    '    val feeds = mutableStateMapOf<String, List<Song>>()\n',
    '''    val feeds = mutableStateMapOf<String, List<Song>>()
    val ytHome = mutableStateListOf<YtShelf>()
    private var ytHomeLoaded = false
    private var picksLoaded = false
''')
add("State.kt",
    '    fun loadFeed(key: String, query: String) {',
    '''    // the real YouTube Music Home of the signed-in account
    fun loadYtHome() {
        if (!signedIn || ytHomeLoaded) return
        ytHomeLoaded = true
        scope.launch {
            try {
                val shelves = YtAccount.home(cookie)
                ytHome.clear()
                ytHome.addAll(shelves)
            } catch (e: Exception) {
                ytHomeLoaded = false
            }
        }
    }

    // picks based on the songs you play most (used when you are not signed in)
    fun loadPicks() {
        if (picksLoaded || Opts.offline) return
        val seeds = (topPlayed().take(3) + history.take(3)).distinctBy { it.id }.take(4)
        if (seeds.isEmpty()) return
        picksLoaded = true
        scope.launch {
            try {
                val all = ArrayList<Song>()
                for (sd in seeds) all.addAll(Engine.related(sd.url).take(12))
                val seen = HashSet<String>()
                val mixed = all.filter { it.id !in counts && seen.add(it.id) }.shuffled().take(20)
                if (mixed.size >= 4) feeds["picks"] = mixed
            } catch (e: Exception) {
                picksLoaded = false
            }
        }
    }

    fun loadFeed(key: String, query: String) {''')

# ---------------- Screens.kt: Home uses your own recommendations
add("Screens.kt",
    '    val hero = quick?.firstOrNull() ?: speedDial.firstOrNull() ?: history.firstOrNull()',
    '''    LaunchedEffect(s.signedIn, Opts.offline, history.size > 0) {
        if (!Opts.offline) {
            s.loadYtHome()
            if (!s.signedIn) s.loadPicks()
        }
    }
    val ytQuick = s.ytHome.firstOrNull { it.title.contains("Quick picks", true) }?.songs
    val picks = if (s.signedIn) null else s.feeds["picks"]
    val bestQuick = ytQuick ?: picks ?: quick
    val hero = bestQuick?.firstOrNull() ?: speedDial.firstOrNull() ?: history.firstOrNull()''')
add("Screens.kt",
    '                item { FeaturedHero(hero, quick ?: listOf(hero), s) }',
    '                item { FeaturedHero(hero, bestQuick ?: listOf(hero), s) }')
add("Screens.kt",
    '''            item { HomeSection("Quick picks") }
            item {
                if (quick == null) LoadingRow(s) else QuickPicksGrid(quick, s)
            }''',
    '''            item { HomeSection(if (ytQuick == null && picks != null) "Picked for you" else "Quick picks") }
            item {
                if (bestQuick == null) LoadingRow(s) else QuickPicksGrid(bestQuick, s)
            }
            // the other shelves of your YouTube Music Home (Listen again, and so on)
            s.ytHome.filter { !it.title.contains("Quick picks", true) }.take(6).forEach { sh ->
                item { HomeSection(sh.title) }
                item { HomeShelf(sh.songs, s) }
            }''')

# ---------------- Settings2.kt: version and changelog
add("Settings2.kt",
    'PfInfo(Icons.Rounded.Info, "Version", "v26.1", a)',
    'PfInfo(Icons.Rounded.Info, "Version", "v27", a)')
add("Settings2.kt",
    '    PfChange("v26.1", a, listOf(',
    '''    PfChange("v27", a, listOf(
        "Home recommendations come from your own YouTube Music Home: Quick picks, Listen again and more",
        "Not signed in: Picked for you, based on the songs you play most"
    ))
    PfChange("v26.1", a, listOf(''')

new_contents = {}
bad = False
for fname, lst in edits.items():
    path = ROOT + fname
    if not os.path.exists(path):
        print("MISSING FILE:", path)
        bad = True
        continue
    text = open(path, encoding="utf-8").read()
    for old, new in lst:
        n = text.count(old)
        if n != 1:
            print("PROBLEM in", fname, "- found", n, "times:", old[:70].replace("\n", " | "))
            bad = True
        else:
            text = text.replace(old, new)
    new_contents[fname] = text

if bad:
    print("\nNothing was changed. Send me the lines above.")
    sys.exit(1)

for fname, text in new_contents.items():
    open(ROOT + fname, "w", encoding="utf-8").write(text)
    print("patched", fname)
print("\nDone. Now: cd ~/PrismWave && git add -A && git commit -m v27 && git push")
