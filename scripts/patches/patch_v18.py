import os, re, sys

D = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
names = ["Player.kt", "Screens.kt", "State.kt", "Pages.kt", "Engine.kt", "PlaybackService.kt"]
files = {n: open(D + n, encoding="utf-8").read() for n in names}


def rep(n, old, new):
    if old not in files[n]:
        sys.exit("MISSING in " + n + ": " + old[:90])
    files[n] = files[n].replace(old, new, 1)


def sub(n, pattern, new):
    t, k = re.subn(pattern, lambda m: new, files[n], count=1, flags=re.S)
    if k != 1:
        sys.exit("MISSING pattern in " + n + ": " + pattern[:90])
    files[n] = t


def sub_after(n, pattern, extra):
    m = re.search(pattern, files[n], flags=re.S)
    if not m:
        sys.exit("MISSING pattern in " + n + ": " + pattern[:90])
    files[n] = files[n][:m.end()] + extra + files[n][m.end():]


def cut(n, start, end, new):
    t = files[n]
    i = t.find(start)
    if i < 0:
        sys.exit("MISSING start in " + n + ": " + start[:90])
    j = t.find(end, i + 1)
    if j < 0:
        sys.exit("MISSING end in " + n + ": " + end[:90])
    files[n] = t[:i] + new + t[j:]


def imports(n, text):
    files[n] = files[n].replace("package com.prismwave.app\n", "package com.prismwave.app\n\n" + text, 1)


# ===================================================== Engine.kt: related songs (radio)
rep("Engine.kt", "    suspend fun collection(url: String): List<Song>",
    '''    suspend fun related(url: String): List<Song> = withContext(Dispatchers.IO) {
        init()
        org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(ServiceList.YouTube, url)
            .relatedItems.filterIsInstance<StreamInfoItem>().filter { it.duration > 0 }.map { it.toSong() }
    }

    suspend fun collection(url: String): List<Song>''')

# ===================================================== PlaybackService.kt: equalizer hookup
imports("PlaybackService.kt", "import androidx.media3.common.Player\n")
rep("PlaybackService.kt", "        session = MediaSession.Builder(this, player).build()",
    '''        Opts.init(this)
        EqHolder.attach(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                EqHolder.attach(audioSessionId)
            }
        })
        session = MediaSession.Builder(this, player).build()''')

# ===================================================== State.kt
rep("State.kt", "    private var pausedAt = 0L\n", "    private var pausedAt = 0L\n    private var fading = false\n    var radio = false\n")
rep("State.kt", "        if (from !== queue) planned.clear()\n",
    "        if (from !== queue) {\n            planned.clear()\n            radio = false\n        }\n        fading = false\n")
rep("State.kt", "                if (Opts.fade) c.volume = 0f\n                c.play()\n                if (Opts.fade) fade(0f, 1f, 450) else c.volume = 1f\n",
    "                val soft = Opts.fade || Opts.crossfade > 0\n                if (soft) c.volume = 0f\n                c.play()\n                if (soft) fade(0f, 1f, if (Opts.crossfade > 0) Opts.crossfade * 1000L else 450L) else c.volume = 1f\n")
sub("State.kt", r'fun next\(\) \{.*?\n    \}\n',
    '''fun next() {
        val cur = current ?: return
        if (queue.isEmpty()) return
        val i = queue.indexOfFirst { it.id == cur.id }
        if (radio && !shuffle && i == queue.size - 1) {
            scope.launch {
                val more = try {
                    Engine.related(cur.url).filter { r -> queue.none { q -> q.id == r.id } }
                } catch (e: Exception) {
                    emptyList()
                }
                if (more.isNotEmpty()) {
                    setQueue(queue + more)
                    skipTo(more[0])
                } else {
                    skipTo(queue[0])
                }
            }
            return
        }
        val n = if (shuffle) (planned.firstOrNull { it.id != cur.id } ?: queue.random()) else queue[(i + 1) % queue.size]
        planned.remove(n)
        skipTo(n)
    }
''')
rep("State.kt", "    fun topPlayed(): List<Song> =",
    '''    fun setQueue(l: List<Song>) {
        queue = l
        planned.clear()
    }

    fun playNext(song: Song) {
        if (current == null) {
            play(song)
            return
        }
        val l = queue.filter { it.id != song.id }.toMutableList()
        val i = l.indexOfFirst { it.id == current?.id }
        l.add(i + 1, song)
        setQueue(l)
    }

    fun addToQueue(song: Song) {
        if (current == null) {
            play(song)
            return
        }
        setQueue(queue.filter { it.id != song.id } + song)
    }

    fun topArtists(): List<Pair<String, Int>> =
        counts.entries.mapNotNull { e -> known[e.key]?.let { it.artist to e.value } }
            .groupBy({ it.first }, { it.second })
            .map { it.key to it.value.sum() }
            .sortedByDescending { it.second }
            .take(5)

    fun topPlayed(): List<Song> =''')
rep("State.kt", "            if (dur > 0) fixDuration((dur / 1000).toInt())\n",
    '''            if (dur > 0) fixDuration((dur / 1000).toInt())
            if (Opts.crossfade > 0 && dur > 0 && !fading && !repeat) {
                val left = dur / 1000f - position
                val cf = Opts.crossfade.toFloat()
                if (dur / 1000f > cf * 3 && left >= 0.5f && left <= cf) {
                    fading = true
                    fade(c.volume, 0f, (left * 1000).toLong() - 150)
                }
            }
''')

# ===================================================== Pages.kt
imports("Pages.kt",
    "import androidx.compose.animation.core.animateFloatAsState\n"
    "import androidx.compose.foundation.layout.PaddingValues\n"
    "import androidx.compose.foundation.lazy.itemsIndexed\n"
    "import androidx.compose.foundation.lazy.rememberLazyListState\n"
    "import androidx.compose.material.icons.rounded.Close\n"
    "import androidx.compose.material.icons.rounded.KeyboardArrowDown\n"
    "import androidx.compose.material.icons.rounded.KeyboardArrowUp\n"
    "import androidx.compose.material.icons.rounded.Pause\n"
    "import androidx.compose.material.icons.rounded.QueueMusic\n"
    "import androidx.compose.material.icons.rounded.SkipNext\n"
    "import androidx.compose.material.icons.rounded.SkipPrevious\n"
    "import androidx.compose.material3.Slider\n"
    "import androidx.compose.material3.Switch\n"
    "import androidx.compose.runtime.DisposableEffect\n"
    "import androidx.compose.runtime.mutableStateListOf\n"
    "import androidx.compose.ui.graphics.TransformOrigin\n"
    "import androidx.compose.ui.platform.LocalView\n")
rep("Pages.kt", "    var headphones by mutableStateOf(true)\n",
    "    var headphones by mutableStateOf(true)\n    var crossfade by mutableStateOf(0)\n    var eqOn by mutableStateOf(false)\n    var eqLevels by mutableStateOf(\"\")\n    var bassBoost by mutableStateOf(0)\n")
rep("Pages.kt", '        headphones = p.getBoolean("headphones", true)\n',
    '        headphones = p.getBoolean("headphones", true)\n        crossfade = p.getInt("crossfade", 0)\n        eqOn = p.getBoolean("eqOn", false)\n        eqLevels = p.getString("eqLevels", "") ?: ""\n        bassBoost = p.getInt("bassBoost", 0)\n')
rep("Pages.kt", '            ?.putBoolean("headphones", headphones)\n',
    '            ?.putBoolean("headphones", headphones)\n            ?.putInt("crossfade", crossfade)\n            ?.putBoolean("eqOn", eqOn)\n            ?.putString("eqLevels", eqLevels)\n            ?.putInt("bassBoost", bassBoost)\n')
rep("Pages.kt", '            "collection" -> CollectionPage(s)\n',
    '            "collection" -> CollectionPage(s)\n            "lyrics" -> LyricsPage(s)\n            "eq" -> EqPage(s)\n            "car" -> CarPage(s)\n')
# artist pages: reload when the artist changes
files["Pages.kt"] = re.sub(r'(remember|LaunchedEffect)\(url\)', r'\1(url + title)', files["Pages.kt"])
# play next / add to queue in the song menu
rep("Pages.kt", '            PageRow(Icons.Rounded.PlaylistAdd, "Add to playlist") { Pages.show("add", song) }\n',
    '''            PageRow(Icons.Rounded.QueueMusic, "Play next") {
                s.playNext(song)
                Toast.makeText(ctx, "Plays next", Toast.LENGTH_SHORT).show()
                Pages.close()
            }
            PageRow(Icons.Rounded.QueueMusic, "Add to end of queue") {
                s.addToQueue(song)
                Toast.makeText(ctx, "Added to queue", Toast.LENGTH_SHORT).show()
                Pages.close()
            }
            PageRow(Icons.Rounded.PlaylistAdd, "Add to playlist") { Pages.show("add", song) }
''')
# queue page with reorder
cut("Pages.kt", "@Composable\nprivate fun QueuePage(", "@Composable\nprivate fun SleepPage(",
'''@Composable
private fun SmallIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun QueuePage(s: AppState) {
    val song = s.current
    var rev by remember { mutableStateOf(0) }
    val list = remember(rev) { s.queue.take(100) }
    fun move(i: Int, d: Int) {
        val l = s.queue.toMutableList()
        val t = i + d
        if (i !in l.indices || t !in l.indices) return
        val x = l.removeAt(i)
        l.add(t, x)
        s.setQueue(l)
        rev++
    }
    SlidePage(s, "Queue", scroll = false) {
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(list) { i, q ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { s.play(q, s.queue) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cover(q, Modifier.size(46.dp), 12.dp, 16.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            q.title,
                            color = if (song != null && q.id == song.id) s.accent else Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(q.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1)
                    }
                    SmallIcon(Icons.Rounded.KeyboardArrowUp) { move(i, -1) }
                    SmallIcon(Icons.Rounded.KeyboardArrowDown) { move(i, 1) }
                    SmallIcon(Icons.Rounded.Close) {
                        s.setQueue(s.queue.filterIndexed { k, _ -> k != i })
                        rev++
                    }
                }
            }
        }
    }
}

''')
# recap + lyrics + equalizer + car mode
cut("Pages.kt", "@Composable\nprivate fun StatsPage(",
    "// ------------------------------------------------------------ search: albums, playlists, artists",
'''@Composable
private fun StatsPage(s: AppState) {
    val top = remember { s.topPlayed().take(5) }
    val artists = remember { s.topArtists() }
    SlidePage(s, "Your recap") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Songs played", "" + s.playCount, Modifier.weight(1f))
            StatCard("Minutes listened", "" + (s.listenSeconds / 60), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Liked songs", "" + s.liked.size, Modifier.weight(1f))
            StatCard("Playlists", "" + s.playlists.size, Modifier.weight(1f))
        }
        if (top.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("Top songs", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            PageCard {
                top.forEachIndexed { i, sg ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { s.play(sg, top) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text((i + 1).toString(), color = s.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                        Cover(sg, Modifier.size(44.dp), 10.dp, 14.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(sg.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(sg.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        if (artists.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("Top artists", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            PageCard {
                artists.forEachIndexed { i, a ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text((i + 1).toString(), color = s.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                        Text(a.first, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(a.second.toString() + " plays", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
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
        if (active >= 0) listState.animateScrollToItem((active - 3).coerceAtLeast(0))
    }
    SlidePage(s, "Lyrics", scroll = false) {
        if (song != null) {
            Text(song.title, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (busy) {
            LoadingRow(s)
        } else if (lines.isEmpty()) {
            Text("No lyrics found for this song", color = TextDim, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(vertical = 40.dp)) {
                itemsIndexed(lines) { i, ln ->
                    val on = i == active
                    val a by animateFloatAsState(if (!synced || on) 1f else 0.38f, tween(250), label = "lya")
                    val sc by animateFloatAsState(if (on) 1f else 0.94f, tween(250), label = "lys")
                    Text(
                        if (ln.text.isEmpty()) "♪" else ln.text,
                        color = (if (on) s.accent else Color.White).copy(alpha = a),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = synced) { s.seekTo(ln.ms / 1000f) }
                            .padding(vertical = 10.dp)
                            .graphicsLayer {
                                scaleX = sc
                                scaleY = sc
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun EqPage(s: AppState) {
    val eq = EqHolder.eq
    var on by remember { mutableStateOf(Opts.eqOn) }
    var bass by remember { mutableStateOf(Opts.bassBoost.toFloat()) }
    SlidePage(s, "Equalizer") {
        if (eq == null) {
            Text("Play a song first, then open the equalizer again.", color = TextDim, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            val lo = eq.bandLevelRange[0].toInt()
            val hi = eq.bandLevelRange[1].toInt()
            val n = eq.numberOfBands.toInt()
            val levels = remember {
                mutableStateListOf<Float>().also { l -> for (b in 0 until n) l.add(eq.getBandLevel(b.toShort()).toFloat()) }
            }
            fun ensureOn() {
                if (!on) {
                    on = true
                    Opts.eqOn = true
                    EqHolder.setEnabled(true)
                    Opts.save()
                }
            }
            PageCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Equalizer", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = on,
                        onCheckedChange = {
                            on = it
                            Opts.eqOn = it
                            EqHolder.setEnabled(it)
                            Opts.save()
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Flat", "Bass", "Vocal", "Treble").forEach { name ->
                    Chip(name, false, s.accent) {
                        val v = EqHolder.preset(name, n, lo, hi)
                        for (b in 0 until n) {
                            levels[b] = v[b]
                            EqHolder.setBand(b, v[b].toInt())
                        }
                        ensureOn()
                        EqHolder.saveLevels()
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PageCard {
                for (b in 0 until n) {
                    val hz = eq.getCenterFreq(b.toShort()) / 1000
                    Text(if (hz >= 1000) "%.1f kHz".format(hz / 1000f) else hz.toString() + " Hz", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Slider(
                        value = levels[b].coerceIn(lo.toFloat(), hi.toFloat()),
                        onValueChange = {
                            levels[b] = it
                            EqHolder.setBand(b, it.toInt())
                        },
                        valueRange = lo.toFloat()..hi.toFloat(),
                        enabled = on,
                        onValueChangeFinished = { EqHolder.saveLevels() }
                    )
                }
            }
            if (EqHolder.bass != null) {
                Spacer(Modifier.height(12.dp))
                PageCard {
                    Text("Bass boost", color = Color.White, fontSize = 18.sp)
                    Slider(
                        value = bass,
                        onValueChange = {
                            bass = it
                            EqHolder.setBass(it.toInt())
                        },
                        valueRange = 0f..1000f,
                        onValueChangeFinished = { Opts.save() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CarButton(icon: ImageVector, size: Dp, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(size).bounceClick(onClick).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
private fun CarPage(s: AppState) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val song = s.current
    SlidePage(s, "Car mode", scroll = false) {
        if (song == null) {
            Text("Play a song first", color = TextDim, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Cover(song, Modifier.size(220.dp), 36.dp, 80.sp, quality = 800)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(song.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 20.sp, maxLines = 1)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    CarButton(Icons.Rounded.SkipPrevious, 96.dp, Color.White.copy(alpha = 0.14f), Color.White) { s.prev() }
                    CarButton(if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 128.dp, s.accent, Color.Black) { s.toggle() }
                    CarButton(Icons.Rounded.SkipNext, 96.dp, Color.White.copy(alpha = 0.14f), Color.White) { s.next() }
                }
            }
        }
    }
}

''')

# ===================================================== Player.kt
imports("Player.kt",
    "import androidx.compose.material.icons.rounded.Subtitles\nimport androidx.compose.material.icons.rounded.DirectionsCar\n")
sub("Player.kt", r'val r = Engine\.search\(song\.artist \+ " " \+ song\.title\)\s*s\.queue = listOf\(song\) \+ r\.filter \{ it\.id != song\.id \}',
    '''val rel = try {
                        Engine.related(song.url)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val r = if (rel.size >= 5) rel else Engine.search(song.artist + " " + song.title)
                    s.setQueue(listOf(song) + r.filter { it.id != song.id })
                    s.radio = true
                    toast("Radio ready")''')
rep("Player.kt", "        }\n    )\n    val maxH",
    '''        },
        MenuAction(Icons.Rounded.Subtitles, "Lyrics", false) { close(); Pages.show("lyrics") },
        MenuAction(Icons.Rounded.DirectionsCar, "Car mode", false) { close(); Pages.show("car") }
    )
    val maxH''')
sub("Player.kt", r'MenuRow\(Icons\.Rounded\.Person, "View artist"\) \{.*?onDismiss\(\)\s*\}',
    'MenuRow(Icons.Rounded.Person, "View artist", song.artist) { Pages.openCollection(song.artist, "", 3) }')
sub("Player.kt", r'MenuRow\(Icons\.Rounded\.Equalizer, "Equalizer"\) \{.*?No system equalizer found on this phone"\)\s*\}\s*\}',
    'MenuRow(Icons.Rounded.Equalizer, "Equalizer") { Pages.show("eq") }')

# ===================================================== Screens.kt
sub_after("Screens.kt", r'ToggleRow\("Offline mode".*?\n',
    '''                        Text(
                            "Fade between songs: " + (if (Opts.crossfade == 0) "off" else Opts.crossfade.toString() + " s"),
                            color = TextDim
                        )
                        Slider(
                            value = Opts.crossfade.toFloat(),
                            onValueChange = { Opts.crossfade = it.toInt() },
                            valueRange = 0f..12f,
                            steps = 11,
                            onValueChangeFinished = { Opts.save() }
                        )
''')
rep("Screens.kt", 'Section("v17") {',
    '''Section("v18") {
                        ChangeList(listOf(
                            "Synced lyrics (Options, Lyrics)",
                            "In-app equalizer with presets and bass boost",
                            "Fade between songs, slider in Behavior",
                            "Real radio: endless related songs",
                            "Queue: move up/down, remove, Play next, Add to end",
                            "Real artist page, Car mode, recap with top songs and artists"
                        ))
                    }
                    Section("v17") {''')
rep("Screens.kt", 'Text("Version v17"', 'Text("Version v18"')

for n in names:
    open(D + n, "w", encoding="utf-8").write(files[n])
print("v18 patch applied")
