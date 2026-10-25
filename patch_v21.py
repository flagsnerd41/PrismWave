import os, re, sys

D = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
names = ["State.kt", "Screens.kt", "Pages.kt", "Player.kt", "PlaybackService.kt"]
files = {n: open(D + n, encoding="utf-8").read() for n in names}


def rep(n, old, new):
    if old not in files[n]:
        sys.exit("MISSING in " + n + ": " + old[:90])
    files[n] = files[n].replace(old, new, 1)


def sub(n, pattern, new, must=True):
    t, k = re.subn(pattern, lambda m: new, files[n], count=1, flags=re.S)
    if k != 1:
        if must:
            sys.exit("MISSING pattern in " + n + ": " + pattern[:90])
        print("skipped: " + pattern[:60])
        return
    files[n] = t


# ================= State.kt: play from real cached files
rep("State.kt", '?: (if (Opts.offline && AudioCache.isFull(ctx, song.id)) "https://cached.invalid/" + song.id else Engine.streamUrl(song.url))',
    "?: SongCache.uri(ctx, song.id) ?: Engine.streamUrl(song.url)")
rep("State.kt", '?: (if (Opts.offline && AudioCache.isFull(ctx, nxt.id)) "https://cached.invalid/" + nxt.id else Engine.streamUrl(nxt.url))',
    "?: SongCache.uri(ctx, nxt.id) ?: Engine.streamUrl(nxt.url)")
rep("State.kt", '                status = ""\n                preload()\n                queueNextItem()\n',
    '                status = ""\n                preload()\n                queueNextItem()\n                cacheCurrent(song, url)\n')
rep("State.kt", "AudioCache.remove(ctx, cur.id)", "SongCache.remove(ctx, cur.id)")
rep("State.kt", "fun cachedSongs(): List<Song> = AudioCache.keys(ctx).mapNotNull { known[it] }",
    "fun cachedSongs(): List<Song> = SongCache.keys(ctx).mapNotNull { known[it] }")
sub("State.kt", r'private fun preload\(\) \{.*?\n    \}\n',
    '''private fun preload() {
        preloadJob?.cancel()
        if (!Opts.preload || Opts.offline) return
        preloadJob = scope.launch {
            delay(2500)
            for (sg in upcoming()) {
                if (DownloadStore.localUri(sg) != null || SongCache.has(ctx, sg.id)) continue
                try {
                    val url = Engine.streamUrl(sg.url)
                    if (SongCache.fetch(ctx, sg.id, url) && queuedId == sg.id) refreshQueued()
                } catch (e: Exception) {
                }
            }
        }
    }

    // the song you are listening to is saved too, so it replays instantly and works offline
    private fun cacheCurrent(song: Song, url: String) {
        if (!url.startsWith("http") || Opts.offline) return
        scope.launch {
            delay(1500)
            try {
                SongCache.fetch(ctx, song.id, url)
            } catch (e: Exception) {
            }
        }
    }
''')
rep("State.kt", "        preload()\n        queueNextItem()\n    }\n\n    private fun onEnded() {",
    '''        preload()
        queueNextItem()
        if (!SongCache.has(ctx, sg.id) && !Opts.offline) {
            scope.launch {
                try {
                    SongCache.fetch(ctx, sg.id, Engine.streamUrl(sg.url))
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun onEnded() {''')

# ================= PlaybackService: the old exo cache is no longer used
sub("PlaybackService.kt",
    r'\s*\.setMediaSourceFactory\(\s*DefaultMediaSourceFactory\(\s*DefaultDataSource\.Factory\(\s*this,\s*CacheDataSource\.Factory\(\)\s*\.setCache\(AudioCache\.get\(this\)\)\s*\.setUpstreamDataSourceFactory\(DefaultHttpDataSource\.Factory\(\)\)\s*\.setFlags\(CacheDataSource\.FLAG_IGNORE_CACHE_ON_ERROR\)\s*\)\s*\)\s*\)',
    "", must=False)

# ================= Screens.kt: settings use the new cache
files["Screens.kt"] = files["Screens.kt"].replace("AudioCache.", "SongCache.")
rep("Screens.kt", "The size limit applies after you restart the app.", "Oldest songs are removed first when the limit is reached.")
rep("Screens.kt", 'Section("v20") {',
    '''Section("v21") {
                        ChangeList(listOf(
                            "Cache rebuilt: songs are saved as real files and play from there",
                            "Lyrics button on the player and a full-screen karaoke view"
                        ))
                    }
                    Section("v20") {''')
rep("Screens.kt", 'Text("Version v20"', 'Text("Version v21"')

# ================= Player.kt: lyrics buttons
rep("Player.kt", "            PlayerIcon(Icons.Rounded.Add, Color.White.copy(alpha = 0.7f)) { onAdd() }\n",
    "            PlayerIcon(Icons.Rounded.Add, Color.White.copy(alpha = 0.7f)) { onAdd() }\n"
    "            PlayerIcon(Icons.Rounded.Subtitles, Color.White.copy(alpha = 0.7f)) { Pages.show(\"lyrics\") }\n")
rep("Player.kt", "                    PlayerIcon(Icons.Rounded.Add, Color.White) { onAdd() }\n",
    "                    PlayerIcon(Icons.Rounded.Add, Color.White) { onAdd() }\n"
    "                    Box(Modifier.width(1.dp).height(26.dp).background(Color.White.copy(alpha = 0.25f)))\n"
    "                    PlayerIcon(Icons.Rounded.Subtitles, Color.White) { Pages.show(\"lyrics\") }\n")

# ================= Pages.kt: full-screen karaoke lyrics
t = files["Pages.kt"]
i = t.find("@Composable\nprivate fun LyricsPage(")
j = t.find("@Composable\nprivate fun EqPage(")
if i < 0 or j < 0:
    sys.exit("MISSING LyricsPage")
files["Pages.kt"] = t[:i] + '''@Composable
private fun LyricsPage(s: AppState) {
    val song = s.current
    var lines by remember(song?.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var busy by remember(song?.id) { mutableStateOf(true) }
    LaunchedEffect(song?.id) {
        busy = true
        lines = if (song != null && !Opts.offline) Lyrics.fetch(song) else emptyList()
        busy = false
    }
    val synced = lines.isNotEmpty() && lines[0].ms >= 0
    val posMs = (s.position * 1000).toLong()
    val active = if (synced) lines.indexOfLast { it.ms <= posMs + 200 } else -1
    val listState = rememberLazyListState()
    LaunchedEffect(active) {
        if (active >= 0) listState.animateScrollToItem((active - 2).coerceAtLeast(0))
    }
    val total = (song?.seconds ?: 1).coerceAtLeast(1).toFloat()
    SlidePage(s, "", scroll = false) {
        if (song != null) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    song.title,
                    color = Color.White,
                    fontFamily = PrismFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(song.artist, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp, maxLines = 1)
            }
        }
        if (busy) {
            LoadingRow(s)
            Spacer(Modifier.weight(1f))
        } else if (lines.isEmpty()) {
            Text(
                "No lyrics found for this song",
                color = TextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 120.dp)
            ) {
                itemsIndexed(lines) { i, ln ->
                    val on = i == active
                    val a by animateFloatAsState(if (!synced || on) 1f else 0.35f, tween(300), label = "lya")
                    val sc by animateFloatAsState(if (on) 1.08f else 0.92f, tween(300), label = "lys")
                    Text(
                        if (ln.text.isEmpty()) "♪" else ln.text,
                        color = (if (on) s.accent else Color.White).copy(alpha = a),
                        fontFamily = PrismFont,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = synced) { s.seekTo(ln.ms / 1000f) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .graphicsLayer {
                                scaleX = sc
                                scaleY = sc
                            }
                    )
                }
            }
        }
        SeekBar({ s.position }, total, { s.seekTo(it) }, Modifier.fillMaxWidth())
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarButton(Icons.Rounded.SkipPrevious, 56.dp, Color.White.copy(alpha = 0.14f), Color.White) { s.prev() }
            PlayButton(s, 72.dp, 36.dp)
            CarButton(Icons.Rounded.SkipNext, 56.dp, Color.White.copy(alpha = 0.14f), Color.White) { s.next() }
        }
    }
}

''' + t[j:]

# ================= changelog file
p = os.path.expanduser("~/PrismWave/CHANGELOG.md")
if os.path.exists(p):
    c = open(p, encoding="utf-8").read()
    if "## v21" not in c:
        head, _, rest = c.partition("\n")
        c = head + "\n\n## v21\n- Fixed: cache rebuilt, songs are saved as real files and play from there\n- Added: lyrics button on the player, full-screen karaoke lyrics\n" + rest
        open(p, "w", encoding="utf-8").write(c)

for n in names:
    open(D + n, "w", encoding="utf-8").write(files[n])
print("v21 patch applied")
