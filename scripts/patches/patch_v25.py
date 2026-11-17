import os
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

patch("Engine.kt", [
("""    suspend fun videoUrl(videoUrl: String): String? = withContext(Dispatchers.IO) {
        init()
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        val vs = info.videoOnlyStreams.filter { it.isUrl && it.height > 0 }
        val mp4 = vs.filter { it.format == MediaFormat.MPEG_4 }
        val pool = if (mp4.isNotEmpty()) mp4 else vs
        (pool.filter { it.height <= 480 }.maxByOrNull { it.height } ?: pool.minByOrNull { it.height })?.content
    }
""",
"""    // main = best video up to maxH, low = tiny copy for the blurred backdrop
    suspend fun videoPair(videoUrl: String, maxH: Int): Pair<String, String>? = withContext(Dispatchers.IO) {
        init()
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        val vs = info.videoOnlyStreams.filter { it.isUrl && it.height > 0 }
        if (vs.isEmpty()) return@withContext null
        val mp4 = vs.filter { it.format == MediaFormat.MPEG_4 }
        val pool = if (mp4.isNotEmpty()) mp4 else vs
        val main = (pool.filter { it.height <= maxH }.maxByOrNull { it.height } ?: pool.minByOrNull { it.height })?.content
        val low = (pool.filter { it.height <= 240 }.maxByOrNull { it.height } ?: pool.minByOrNull { it.height })?.content
        if (main == null || low == null) null else Pair(main, low)
    }
"""),
])

patch("MainActivity.kt", [
("LiquidGlassOpt.init(appCtx) }", "LiquidGlassOpt.init(appCtx); VideoSync.init(appCtx) }"),
])

patch("Player.kt", [
# full screen sheet: no gap on top
("            .fillMaxSize()\n            .padding(top = 8.dp)\n            .offset { IntOffset(0, dragY.value.roundToInt()) }\n",
 "            .fillMaxSize()\n            .offset { IntOffset(0, dragY.value.roundToInt()) }\n"),
# liquid glass mini player
("        Glass(\n            s.glassAlpha(),\n            Modifier\n                .fillMaxWidth()\n                .drawBehind {",
 "        LiquidGlass(\n            s.glassAlpha(),\n            Modifier\n                .fillMaxWidth()\n                .drawBehind {"),
("            RoundedCornerShape(32.dp)\n        ) {\n            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {",
 "            32.dp\n        ) {\n            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {"),
# moving blurred video backdrop
("        Cover(song, Modifier.fillMaxSize().blur(50.dp), 0.dp, 160.sp, quality = 600)\n",
"""        if (VideoSync.showVideo(song)) {
            VideoStage(s, song, Modifier.fillMaxSize().blur(50.dp), low = true)
        } else {
            Cover(song, Modifier.fillMaxSize().blur(50.dp), 0.dp, 160.sp, quality = 600)
        }
"""),
])

patch("Settings2.kt", [
('    PfLabel("Timer", a)\n',
"""    PfLabel("Video (experiment)", a)
    PfGroup {
        val vctx = LocalContext.current
        PfChoice(
            Icons.Rounded.PlayArrow, "Video quality", VideoSync.qualityNames, VideoSync.quality, a,
            "Higher quality uses more data"
        ) { VideoSync.setQuality(vctx, it) }
    }
    PfLabel("Timer", a)
"""),
('PfInfo(Icons.Rounded.Info, "Version", "v24", a)', 'PfInfo(Icons.Rounded.Info, "Version", "v25", a)'),
('    PfChange("v24", a, listOf(\n',
"""    PfChange("v25", a, listOf(
        "Video: blurred backdrop now moves with the video, high quality by default",
        "Video quality choice in Player and audio",
        "Song/Video pills slide",
        "Player is full screen with rounded top corners",
        "Liquid glass on the mini player",
        "Media notification icon is now your P logo"
    ))
    PfChange("v24", a, listOf(
"""),
])

print("\nERRORS:" if errors else "\nAll patched.")
for e in errors: print(" ", e)
