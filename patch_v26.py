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

patch("MainActivity.kt", [
("    val bgImg = ImageBitmap.imageResource(R.drawable.bg_gradient)\n", ""),
("""                    .drawBehind {
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
""",
"                    .drawBehind { drawRect(Brush.verticalGradient(listOf(tintTop, tintBot))) }\n"),
("    var scrolled by remember { mutableStateOf(0f) }\n",
 "    LaunchedEffect(artC) { if (cur != null && Opts.livingBg) s.accent = lerp(artC, Color.White, 0.35f) }\n    var scrolled by remember { mutableStateOf(0f) }\n"),
])

patch("Screens.kt", [
("import androidx.compose.ui.draw.clip\n",
 "import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.blur\nimport androidx.compose.ui.draw.drawWithContent\nimport androidx.compose.ui.graphics.graphicsLayer\nimport androidx.compose.ui.graphics.CompositingStrategy\nimport androidx.compose.ui.graphics.BlendMode\n"),
("""        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.72f),
                    0.55f to Color.Black.copy(alpha = 0.25f),
                    1f to Color.Transparent
                )
            )
        )
""",
"""        if (!Opts.lite && !s.batterySaver && android.os.Build.VERSION.SDK_INT >= 31) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.horizontalGradient(0f to Color.Black, 0.55f to Color.Black, 0.85f to Color.Transparent),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                Cover(song, Modifier.fillMaxSize().blur(30.dp), 0.dp, 90.sp, quality = 400)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
            }
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.72f),
                        0.55f to Color.Black.copy(alpha = 0.25f),
                        1f to Color.Transparent
                    )
                )
            )
        }
"""),
])
print("\nERRORS:" if errors else "\nAll patched.")
for e in errors: print(" ", e)
