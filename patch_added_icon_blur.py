import os, sys
base = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")

NEW_FN = '''fun Modifier.liquidGlyph(blur: Dp = 0.dp, glow: Color? = null): Modifier = composed {
    val backdrop = LocalBackdrop.current
    val canBlur = Build.VERSION.SDK_INT >= 31
    val lens = GlassIcons.on && backdrop != null && backdrop.enabled && canBlur && !Opts.lite
    if (!lens && !(canBlur && (blur > 0.dp || glow != null))) return@composed this
    val layer = rememberGraphicsLayer()
    val glowLayer = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(Offset.Zero) }
    var m: Modifier = this.onGloballyPositioned { pos = it.positionInRoot() }
    // soft bloom behind the icon (only for the chosen tab): a blurred, tinted copy of the icon, drawn outside the glass layer
    if (glow != null && canBlur) {
        m = m.drawWithContent {
            val gw = size.width.toInt().coerceAtLeast(1)
            val gh = size.height.toInt().coerceAtLeast(1)
            val g = 4.dp.toPx()
            glowLayer.colorFilter = ColorFilter.tint(glow, BlendMode.SrcIn)
            glowLayer.renderEffect = BlurEffect(g, g, TileMode.Decal)
            glowLayer.alpha = 0.8f
            glowLayer.record(this, layoutDirection, IntSize(gw, gh)) { this@drawWithContent.drawContent() }
            drawLayer(glowLayer)
            drawContent()
        }
    }
    // real blur on the icon pixels themselves (not the backdrop, not opacity)
    m = m.graphicsLayer {
        if (lens) compositingStrategy = CompositingStrategy.Offscreen
        if (canBlur && blur > 0.dp) {
            val px = blur.toPx()
            renderEffect = BlurEffect(px, px, TileMode.Decal)
        }
    }
    if (lens && backdrop != null) {
        m = m.drawWithContent {
            // the icon itself is only the shape
            drawContent()
            if (backdrop.size.width > 0) {
                val w = size.width.toInt().coerceAtLeast(1)
                val h = size.height.toInt().coerceAtLeast(1)
                layer.blendMode = BlendMode.SrcIn
                layer.renderEffect = BlurEffect(26f, 26f, TileMode.Clamp)
                layer.colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(2.0f) })
                layer.record(this, layoutDirection, IntSize(w, h)) {
                    val cx = pos.x + w / 2f
                    val cy = pos.y + h / 2f
                    scale(1.8f, pivot = Offset(cx, cy)) {
                        translate(-pos.x, -pos.y) { drawLayer(backdrop.layer) }
                    }
                    drawRect(Color.White.copy(alpha = 0.96f))
                }
                drawLayer(layer)
            }
        }
    }
    m
}

'''

def rd(n):
    return open(base + n, encoding="utf-8").read()

# 1) IosMenu.kt: liquidGlyph gets blur + glow
ios = rd("IosMenu.kt")
a = ios.index("fun Modifier.liquidGlyph(): Modifier = composed {")
b = ios.index("// ------------------------------------------------------------ compact iOS style Now Playing menu")
ios = ios[:a] + NEW_FN + ios[b:]
imp = "import androidx.compose.ui.unit.IntSize\n"
assert imp in ios
if "import androidx.compose.ui.unit.Dp\n" not in ios:
    ios = ios.replace(imp, "import androidx.compose.ui.unit.Dp\n" + imp, 1)

# 2) AnimIcons.kt: tab icons (inactive = soft blur, chosen = sharp + red bloom)
ai = rd("AnimIcons.kt")
old = "}.liquidGlyph()"
assert ai.count(old) == 1
ai = ai.replace(old, "}.liquidGlyph(\n            if (selected) 0.3.dp else 1.dp,\n            if (selected) Color(0xFFFA243C) else null\n        )")

# 3) NavStyles.kt: round Search button
ns = rd("NavStyles.kt")
old = "Modifier.size(28.dp).liquidGlyph()"
assert ns.count(old) == 1
ns = ns.replace(old, "Modifier.size(28.dp).liquidGlyph(\n                    if (searchSel) 0.3.dp else 1.dp,\n                    if (searchSel) Color(0xFFFA243C) else null\n                )")

open(base + "IosMenu.kt", "w", encoding="utf-8").write(ios)
open(base + "AnimIcons.kt", "w", encoding="utf-8").write(ai)
open(base + "NavStyles.kt", "w", encoding="utf-8").write(ns)
print("done: icon blur + glow patched")
