import os, sys

ROOT = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
edits = {}

def add(fname, old, new):
    edits.setdefault(fname, []).append((old, new))

# ---- Engine.kt: ONE fetch per song shared by audio link and video check (this was the lag)
add("Engine.kt",
    '''    fun forget(videoUrl: String) {
        synchronized(urlCache) { urlCache.remove(videoUrl) }
    }''',
    '''    private val infoCache = HashMap<String, Pair<StreamInfo, Long>>()
    private val infoLocks = HashMap<String, kotlinx.coroutines.sync.Mutex>()

    fun forget(videoUrl: String) {
        synchronized(urlCache) { urlCache.remove(videoUrl) }
        synchronized(infoCache) { infoCache.remove(videoUrl) }
    }

    // the same song is only fetched once, even when several parts of the app ask at the same time
    private suspend fun infoFor(videoUrl: String): StreamInfo {
        val lock = synchronized(infoLocks) { infoLocks.getOrPut(videoUrl) { kotlinx.coroutines.sync.Mutex() } }
        lock.lock()
        try {
            val nowMs = System.currentTimeMillis()
            val hit = synchronized(infoCache) { infoCache[videoUrl] }
            if (hit != null && nowMs - hit.second < 4L * 3600 * 1000) return hit.first
            init()
            val fresh = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
            synchronized(infoCache) {
                if (infoCache.size > 8) infoCache.clear()
                infoCache[videoUrl] = Pair(fresh, nowMs)
            }
            return fresh
        } finally {
            lock.unlock()
        }
    }''')
add("Engine.kt",
    '        init()\n        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)\n        val audio = info.audioStreams',
    '        val info = infoFor(videoUrl)\n        val audio = info.audioStreams')
add("Engine.kt",
    '        init()\n        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)\n        val vs = info.videoOnlyStreams',
    '''        val info = infoFor(videoUrl)
        // a "song" (auto-generated Topic upload) only has a still picture, so no video for it
        val isSong = (info.uploaderName ?: "").endsWith(" - Topic") ||
            (info.description?.content ?: "").startsWith("Provided to YouTube by")
        if (isSong) return@withContext null
        val vs = info.videoOnlyStreams''')

# ---- State.kt: the next songs load after the current one is already playing
add("State.kt",
    '            delay(600)\n            for (sg in upcoming()) {',
    '            delay(2500)\n            for (sg in upcoming()) {')

# ---- PlaybackService.kt: still a quick start, but not so tiny that it stalls
add("PlaybackService.kt",
    '.setBufferDurationsMs(20000, 60000, 400, 1200)',
    '.setBufferDurationsMs(20000, 60000, 700, 1800)')

# ---- Player.kt: do not check for video while the player is still sliding open
add("Player.kt",
    '    LaunchedEffect(song.id) { VideoSync.check(song) }',
    '    LaunchedEffect(song.id) {\n        delay(700)\n        VideoSync.check(song)\n    }')

# ---- Lyrics.kt: prefer a synced version (that is what makes the lines animate)
add("Lyrics.kt",
    '            if (result.isEmpty()) {\n                val body2',
    '            if (result.isEmpty() || result[0].ms < 0) {\n                val body2')
add("Lyrics.kt",
    '''                        if (r.isNotEmpty()) {
                            result = r
                            break
                        }''',
    '''                        if (r.isNotEmpty() && (result.isEmpty() || r[0].ms >= 0)) {
                            result = r
                            if (r[0].ms >= 0) break
                        }''')

# ---- Settings2.kt: backup way to sign in to Discord
add("Settings2.kt",
    '        if (DiscordRpc.status.isNotEmpty()) PfNote(DiscordRpc.status)',
    '''        if (!DiscordRpc.hasToken()) {
            PfAction(
                Icons.Rounded.Lock, "Use token from clipboard",
                "If the sign-in page does not work, copy your Discord token and tap here", a
            ) {
                val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val tok = (cm.primaryClip?.getItemAt(0)?.text?.toString() ?: "").trim().trim('"')
                if (tok.length > 40) DiscordRpc.onToken(ctx.applicationContext, tok)
            }
        }
        if (DiscordRpc.status.isNotEmpty()) PfNote(DiscordRpc.status)''')
add("Settings2.kt",
    '    PfChange("v26", a, listOf(',
    '''    PfChange("v26.1", a, listOf(
        "Lyrics: current line grows, others shrink, blur and fade, smooth glide",
        "Lyrics: finds a synced version when the first one is plain text",
        "Discord sign-in page opens as a desktop page, with a clipboard backup",
        "Less lag when opening the player: one fetch per song, video check waits",
        "No video button for songs, only for real videos"
    ))
    PfChange("v26", a, listOf(''')
add("Settings2.kt",
    'PfInfo(Icons.Rounded.Info, "Version", "v26", a)',
    'PfInfo(Icons.Rounded.Info, "Version", "v26.1", a)')

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
print("\nDone. Now: cd ~/PrismWave && git add -A && git commit -m v26.1 && git push")
