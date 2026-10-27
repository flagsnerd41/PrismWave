import os, re
ROOT = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
errors = []

def patch(fname, pairs):
    path = ROOT + fname
    out = open(path, encoding="utf-8").read()
    for i, (old, new) in enumerate(pairs):
        n = out.count(old)
        if n != 1:
            errors.append("%s step %d: %d matches" % (fname, i + 1, n))
            return
        out = out.replace(old, new)
    open(path, "w", encoding="utf-8").write(out)
    print("ok", fname)

patch("MainActivity.kt", [
("import android.os.Build\n",
 "import android.os.Build\nimport androidx.compose.ui.res.imageResource\nimport androidx.compose.ui.graphics.ImageBitmap\nimport androidx.compose.ui.graphics.FilterQuality\nimport androidx.compose.ui.unit.IntOffset\nimport androidx.compose.ui.unit.IntSize\n"),
("            remember { Opts.init(appCtx) }\n",
 "            remember { Opts.init(appCtx); LiquidGlassOpt.init(appCtx) }\n"),
("    val layer = rememberGraphicsLayer()\n    val backdrop = remember { Backdrop(layer) }\n",
 "    val layer = rememberGraphicsLayer()\n    val bgImg = ImageBitmap.imageResource(R.drawable.bg_gradient)\n    val backdrop = remember { Backdrop(layer) }\n"),
("                    .drawBehind { drawRect(Brush.verticalGradient(listOf(tintTop, tintBot))) }\n",
"""                    .drawBehind {
                        if (Opts.theme == 2) {
                            drawRect(Color.Black)
                        } else {
                            val sw = bgImg.width
                            val sh = bgImg.height
                            val sc = maxOf(size.width / sw, size.height / sh)
                            val srcW = minOf(sw, (size.width / sc).toInt().coerceAtLeast(1))
                            val srcH = minOf(sh, (size.height / sc).toInt().coerceAtLeast(1))
                            drawImage(
                                bgImg,
                                srcOffset = IntOffset((sw - srcW) / 2, (sh - srcH) / 2),
                                srcSize = IntSize(srcW, srcH),
                                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                filterQuality = FilterQuality.Medium
                            )
                        }
                    }
"""),
])

patch("Player.kt", [
("Glass(s.glassAlpha(), Modifier.fillMaxWidth().height(78.dp), RoundedCornerShape(30.dp)) {",
 "LiquidGlass(s.glassAlpha(), Modifier.fillMaxWidth().height(78.dp), 30.dp) {"),
("    BackHandler { s.showPlayer = false }\n    val dragY = remember { Animatable(0f) }\n",
 "    BackHandler { s.showPlayer = false }\n    LaunchedEffect(song.id) { VideoSync.check(song) }\n    val dragY = remember { Animatable(0f) }\n"),
("            .fillMaxSize()\n            .offset { IntOffset(0, dragY.value.roundToInt()) }\n",
 "            .fillMaxSize()\n            .padding(top = 8.dp)\n            .offset { IntOffset(0, dragY.value.roundToInt()) }\n"),
("            .background(Night)\n            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}\n    ) {\n        AnimatedContent(",
 "            .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))\n            .background(Night)\n            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}\n    ) {\n        AnimatedContent("),
("        ) { Cover(song, Modifier.fillMaxSize(), 20.dp, 96.sp, quality = 1000) }\n",
"""        ) {
            if (VideoSync.showVideo(song)) {
                VideoStage(s, song, Modifier.fillMaxSize().clip(coverShape))
            } else {
                Cover(song, Modifier.fillMaxSize(), 20.dp, 96.sp, quality = 1000)
            }
        }
"""),
("                .clickable { s.showPlayer = false }\n        )\n        Box(\n            Modifier\n                .fillMaxWidth()\n                .aspectRatio(1f)",
 "                .clickable { s.showPlayer = false }\n        )\n        if (VideoSync.has[song.id] == true) {\n            ModePills()\n            Spacer(Modifier.height(14.dp))\n        }\n        Box(\n            Modifier\n                .fillMaxWidth()\n                .aspectRatio(1f)"),
("            Cover(song, Modifier.fillMaxSize(), 0.dp, 160.sp, quality = 1200)\n",
"""            if (VideoSync.showVideo(song)) {
                VideoStage(s, song, Modifier.fillMaxSize())
            } else {
                Cover(song, Modifier.fillMaxSize(), 0.dp, 160.sp, quality = 1200)
            }
"""),
("""            Text(
                "Playing from PrismWave",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 18.dp).clickable { s.showPlayer = false }
            )
""",
"""            if (VideoSync.has[song.id] == true) {
                Spacer(Modifier.height(10.dp))
                ModePills()
            } else {
                Text(
                    "Playing from PrismWave",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 18.dp).clickable { s.showPlayer = false }
                )
            }
"""),
])

patch("Engine.kt", [
("    suspend fun streamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {\n",
"""    suspend fun videoUrl(videoUrl: String): String? = withContext(Dispatchers.IO) {
        init()
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        val vs = info.videoOnlyStreams.filter { it.isUrl && it.height > 0 }
        val mp4 = vs.filter { it.format == MediaFormat.MPEG_4 }
        val pool = if (mp4.isNotEmpty()) mp4 else vs
        (pool.filter { it.height <= 480 }.maxByOrNull { it.height } ?: pool.minByOrNull { it.height })?.content
    }

    suspend fun streamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
"""),
])

patch("PlaybackService.kt", [
("import androidx.media3.session.MediaSession\n",
 "import androidx.media3.session.DefaultMediaNotificationProvider\nimport androidx.media3.session.MediaSession\n"),
("        session = MediaSession.Builder(this, player).build()\n",
 "        session = MediaSession.Builder(this, player).build()\n        setMediaNotificationProvider(\n            DefaultMediaNotificationProvider(this).apply { setSmallIcon(R.drawable.ic_notif_p) }\n        )\n"),
])

patch("State.kt", [
("    private var retriedId: String? = null\n",
 "    private var retriedId: String? = null\n    private var earlyId: String? = null\n"),
("""    private fun onEnded() {
        val id = current?.id ?: return
        if (endedFor == id) return
        endedFor = id
        if (repeat) {
            endedFor = null
            controller?.seekTo(0)
""",
"""    private fun onEnded() {
        val cur = current ?: return
        val id = cur.id
        if (endedFor == id) return
        endedFor = id
        if (!repeat && earlyId != id && cur.seconds > 30 && position < cur.seconds - 10f) {
            // stopped far too early (broken file or stream): drop the cached copy and continue from a fresh link
            earlyId = id
            endedFor = null
            retriedId = null
            handleError()
            return
        }
        if (repeat) {
            endedFor = null
            fading = false
            fadeJob?.cancel()
            controller?.volume = 1f
            controller?.seekTo(0)
"""),
("                c.prepare()\n                c.play()\n            } catch (e: Exception) {\n                skipBroken()\n",
 "                c.prepare()\n                c.play()\n                queuedId = null\n                queueNextItem()\n            } catch (e: Exception) {\n                skipBroken()\n"),
("            if (dur > 0) fixDuration((dur / 1000).toInt())\n",
 "            if (dur > 0) fixDuration((dur / 1000).toInt())\n            if (!fading && fadeJob?.isActive != true && c.volume < 0.99f) c.volume = 1f\n"),
])

# SongCache: verified, resumable download
p = ROOT + "SongCache.kt"
src = open(p, encoding="utf-8").read()
new_fn = '''    private fun download(ctx: Context, id: String, url: String): Boolean {
        val tmp = File(dir(ctx), name(id) + ".part")
        try {
            tmp.delete()
            var total = -1L
            var got = 0L
            var rounds = 0
            while (rounds < 60) {
                rounds++
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 10000
                c.readTimeout = 20000
                c.setRequestProperty("Range", "bytes=" + got + "-")
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                val code = c.responseCode
                if (code !in 200..299 || (code == 200 && got > 0)) {
                    c.disconnect()
                    tmp.delete()
                    return false
                }
                val cr = c.getHeaderField("Content-Range")
                if (cr != null && cr.contains("/")) {
                    total = cr.substringAfter("/").trim().toLongOrNull() ?: total
                } else if (code == 200) {
                    total = c.contentLengthLong
                }
                var n = 0L
                java.io.FileOutputStream(tmp, got > 0).use { out ->
                    c.inputStream.use { inp ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val r = inp.read(buf)
                            if (r < 0) break
                            out.write(buf, 0, r)
                            n += r
                        }
                    }
                }
                c.disconnect()
                got += n
                if (total <= 0L || got >= total) break
                if (n == 0L) {
                    tmp.delete()
                    return false
                }
            }
            // a song that is not complete would cut off mid-way, so never keep one
            if (total > 0L && got < total) {
                tmp.delete()
                return false
            }
            if (tmp.length() < 50_000) {
                tmp.delete()
                return false
            }
            val dest = file(ctx, id)
            dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.delete()
                return false
            }
            return true
        } catch (e: Exception) {
            tmp.delete()
            return false
        }
    }

'''
pat = re.compile(r"    private fun download\(.*?\n    }\n\n(?=    suspend fun fetch)", re.S)
if len(pat.findall(src)) != 1:
    errors.append("SongCache.kt: download not found")
else:
    open(p, "w", encoding="utf-8").write(pat.sub(lambda m: new_fn, src))
    print("ok SongCache.kt")

patch("Settings2.kt", [
('    PfLabel("Status bar", a)\n',
"""    PfLabel("Experimental", a)
    PfGroup {
        val lctx = LocalContext.current
        val ok = android.os.Build.VERSION.SDK_INT >= 33
        PfSwitch(
            Icons.Rounded.AutoAwesome, "Liquid glass menu bar",
            if (ok) "Real glass refraction on the bottom bar. Off or Lite mode uses the normal blur" else "Needs Android 13 or newer",
            LiquidGlassOpt.on && ok, a
        ) { if (ok) LiquidGlassOpt.set(lctx, it) }
    }
    PfLabel("Status bar", a)
"""),
('PfInfo(Icons.Rounded.Info, "Version", "v23", a)', 'PfInfo(Icons.Rounded.Info, "Version", "v24", a)'),
('    PfChange("v23", a, listOf(\n',
"""    PfChange("v24", a, listOf(
        "New app background: navy to blue to teal to blue gradient",
        "Liquid glass menu bar (Android 13+), own switch in Appearance",
        "Full player is a sheet with rounded top corners",
        "App icon zoomed a bit, media notification icon is a white P",
        "Song and Video pills in the player (experiment)",
        "Safer song cache and recovery if a song stops too early"
    ))
    PfChange("v23", a, listOf(
"""),
])

print("\nERRORS:" if errors else "\nAll patched.")
for e in errors: print(" ", e)
