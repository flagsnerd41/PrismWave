import os, sys

D = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
names = ["State.kt", "Screens.kt"]
files = {n: open(D + n, encoding="utf-8").read() for n in names}


def rep(n, old, new):
    if old not in files[n]:
        sys.exit("MISSING in " + n + ": " + old[:90])
    files[n] = files[n].replace(old, new, 1)


# --- fields
rep("State.kt", "    private var fading = false\n    var radio = false\n",
    "    private var fading = false\n    private var queuedId: String? = null\n    private var autoAdvance = false\n    var radio = false\n")

# --- changing the queue refreshes the song that is already lined up
rep("State.kt", "    fun replaceQueue(l: List<Song>) {\n        queue = l\n        planned.clear()\n    }\n",
    "    fun replaceQueue(l: List<Song>) {\n        queue = l\n        planned.clear()\n        refreshQueued()\n    }\n")

# --- the player moves to the next song by itself; the app just follows along
rep("State.kt", "                    override fun onPlayerError(error: PlaybackException) {\n",
    '''                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return
                        val sg = mediaItem?.mediaId?.let { known[it] } ?: return
                        if (sg.id == current?.id) return
                        onAutoAdvance(sg)
                    }

                    override fun onPlayerError(error: PlaybackException) {
''')

# --- play(): no placeholder link unless offline, no fade-in when the song follows on by itself
rep("State.kt", "        fading = false\n        pausedByUnplug = false\n        current = song\n",
    "        fading = false\n        queuedId = null\n        pausedByUnplug = false\n        current = song\n")
rep("State.kt", '?: (if (AudioCache.isFull(ctx, song.id)) "https://cached.invalid/" + song.id else Engine.streamUrl(song.url))',
    '?: (if (Opts.offline && AudioCache.isFull(ctx, song.id)) "https://cached.invalid/" + song.id else Engine.streamUrl(song.url))')
rep("State.kt", "                val soft = Opts.fade || Opts.crossfade > 0\n",
    "                val soft = (Opts.fade || Opts.crossfade > 0) && !(autoAdvance && Opts.crossfade == 0)\n                autoAdvance = false\n")
rep("State.kt", '                status = ""\n                preload()\n',
    '                status = ""\n                preload()\n                queueNextItem()\n')

# --- fallback path (only used when the next song was not lined up in time)
rep("State.kt", "            controller?.play()\n        } else {\n            next()\n        }\n",
    "            controller?.play()\n        } else {\n            autoAdvance = true\n            next()\n        }\n")

# --- new helpers
rep("State.kt", "    private fun onEnded() {\n",
    '''    // The next song is added to the player's own list while the current one plays,
    // so Android never sees the audio stop between songs (this is what muted it in the background).
    private fun queueNextItem() {
        val c = controller ?: return
        val cur = current ?: return
        if (repeat) return
        if (radio && !shuffle && queue.lastOrNull()?.id == cur.id) {
            scope.launch {
                try {
                    val more = Engine.related(cur.url).filter { r -> queue.none { q -> q.id == r.id } }
                    if (more.isNotEmpty() && current?.id == cur.id) replaceQueue(queue + more)
                } catch (e: Exception) {
                }
            }
            return
        }
        val nxt = upcoming().firstOrNull() ?: return
        if (queuedId == nxt.id) return
        scope.launch {
            try {
                val url = DownloadStore.localUri(nxt)
                    ?: (if (Opts.offline && AudioCache.isFull(ctx, nxt.id)) "https://cached.invalid/" + nxt.id else Engine.streamUrl(nxt.url))
                if (current?.id != cur.id || upcoming().firstOrNull()?.id != nxt.id) return@launch
                while (c.mediaItemCount > c.currentMediaItemIndex + 1) c.removeMediaItem(c.mediaItemCount - 1)
                val meta = MediaMetadata.Builder()
                    .setTitle(nxt.title)
                    .setArtist(nxt.artist)
                    .setArtworkUri(nxt.thumb?.let { Uri.parse(artUrl(it, 800)) })
                    .build()
                c.addMediaItem(MediaItem.Builder().setUri(url).setMediaId(nxt.id).setCustomCacheKey(nxt.id).setMediaMetadata(meta).build())
                queuedId = nxt.id
            } catch (e: Exception) {
            }
        }
    }

    private fun refreshQueued() {
        val c = controller ?: return
        if (current == null) return
        try {
            while (c.mediaItemCount > c.currentMediaItemIndex + 1) c.removeMediaItem(c.mediaItemCount - 1)
        } catch (e: Exception) {
        }
        queuedId = null
        queueNextItem()
    }

    private fun onAutoAdvance(sg: Song) {
        planned.removeAll { it.id == sg.id }
        fading = false
        current = sg
        position = 0f
        status = ""
        loading = false
        endedFor = null
        retriedId = null
        queuedId = null
        playCount++
        known[sg.id] = sg
        counts[sg.id] = (counts[sg.id] ?: 0) + 1
        history.removeAll { it.id == sg.id }
        history.add(0, sg)
        while (history.size > 30) history.removeAt(history.size - 1)
        saveAll()
        if (Opts.crossfade > 0) {
            fade(0f, 1f, Opts.crossfade * 1000L)
        } else {
            fadeJob?.cancel()
            controller?.volume = 1f
        }
        preload()
        queueNextItem()
    }

    private fun onEnded() {
''')

# --- changelog
rep("Screens.kt", 'Section("v19") {',
    '''Section("v20") {
                        ChangeList(listOf(
                            "Fix: audio cutting a few seconds into the next song when the app is in the background",
                            "The player now moves to the next song by itself, so playback never stops in between"
                        ))
                    }
                    Section("v19") {''')
rep("Screens.kt", 'Text("Version v19"', 'Text("Version v20"')

for n in names:
    open(D + n, "w", encoding="utf-8").write(files[n])
print("v20 patch applied")
