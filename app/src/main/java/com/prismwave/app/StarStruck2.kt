package com.prismwave.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Drawn in a 1000 x 2200 space and scaled to the screen.
private const val SS_DUR = 9.0f
private const val SS_W = 1000f
private const val SS_H = 2200f

private class SsLeaf(val path: Path, val dark: Boolean, val phase: Float)

private fun ssLeaf(cx: Float, cy: Float, len: Float, wid: Float, deg: Float): Path {
    val r = deg * 0.017453292f
    val dx = cos(r)
    val dy = sin(r)
    val px = -dy
    val py = dx
    val p = Path()
    p.moveTo(cx - dx * len / 2f, cy - dy * len / 2f)
    p.quadraticBezierTo(cx + px * wid, cy + py * wid, cx + dx * len / 2f, cy + dy * len / 2f)
    p.quadraticBezierTo(cx - px * wid, cy - py * wid, cx - dx * len / 2f, cy - dy * len / 2f)
    p.close()
    return p
}

private fun ssBuildLeaves(): List<SsLeaf> {
    val r = Random(33)
    val out = ArrayList<SsLeaf>()
    // x range, y range, angle range (degrees, 0 = right, 90 = down), count
    val groups = listOf(
        floatArrayOf(-60f, 220f, 150f, 1100f, -40f, 50f, 34f),
        floatArrayOf(-60f, 1060f, -60f, 240f, 55f, 125f, 24f),
        floatArrayOf(600f, 1060f, 100f, 700f, 115f, 205f, 34f),
        floatArrayOf(840f, 1060f, 500f, 1250f, 150f, 215f, 22f)
    )
    for (g in groups) {
        for (i in 0 until g[6].toInt()) {
            val cx = g[0] + r.nextFloat() * (g[1] - g[0])
            val cy = g[2] + r.nextFloat() * (g[3] - g[2])
            val deg = g[4] + r.nextFloat() * (g[5] - g[4])
            val len = 90f + r.nextFloat() * 150f
            val wid = 22f + r.nextFloat() * 34f
            out.add(SsLeaf(ssLeaf(cx, cy, len, wid, deg), r.nextBoolean(), r.nextFloat() * 6.28f))
        }
    }
    return out
}

private fun DrawScope.ssPine(cx: Float, top: Float, base: Float, halfW: Float, col: Color, rim: Boolean, t: Float, a: Float) {
    val tiers = 8
    val step = (base - top) / tiers
    for (k in 0 until tiers) {
        val yTop = top + step * k * 0.92f
        val yBot = yTop + step * 1.9f
        val hw = halfW * (0.25f + 0.75f * (k + 1) / tiers)
        val p = Path()
        p.moveTo(cx, yTop)
        p.lineTo(cx + hw, yBot)
        p.lineTo(cx - hw, yBot)
        p.close()
        drawPath(p, col)
        if (rim) {
            val fl = 0.65f + 0.35f * sin(t * 2.6f + k * 1.7f)
            drawLine(
                Color(0xFFE0652B).copy(alpha = (0.85f * fl * a).coerceIn(0f, 1f)),
                Offset(cx, yTop),
                Offset(cx + hw, yBot),
                strokeWidth = 4f
            )
            drawCircle(Color(0xFFFF9A4A).copy(alpha = (0.7f * fl * a).coerceIn(0f, 1f)), 5f, Offset(cx + hw * 0.8f, yTop + (yBot - yTop) * 0.8f))
        }
    }
}

@Composable
fun StarStruckOverlay2(modifier: Modifier = Modifier) {
    var t by remember { mutableStateOf(-1f) }
    LaunchedEffect(Unit) {
        val first = StudioPrefs.lastStar == 0L
        val since = System.currentTimeMillis() - StudioPrefs.lastStar
        delay(if (first) 3000L else (60000L - since).coerceAtLeast(600L))
        while (true) {
            StudioPrefs.lastStar = System.currentTimeMillis()
            val start = withFrameNanos { it }
            var el = 0f
            while (el < SS_DUR) {
                val now = withFrameNanos { it }
                el = (now - start) / 1_000_000_000f
                t = el
            }
            t = -1f
            delay(50000L)
        }
    }
    val leaves = remember { ssBuildLeaves() }
    val stars = remember {
        val r = Random(41)
        List(150) { floatArrayOf(r.nextFloat() * SS_W, r.nextFloat() * 1450f, r.nextFloat() * 6.28f, 1.2f + r.nextFloat() * 2.4f) }
    }
    // x, y, length, width, angle, drift x, drift y, delay, life
    val sparks = remember {
        val r = Random(7)
        List(16) {
            floatArrayOf(
                170f + r.nextFloat() * 700f,
                260f + r.nextFloat() * 900f,
                70f + r.nextFloat() * 90f,
                14f + r.nextFloat() * 12f,
                r.nextFloat() * 180f,
                -40f + r.nextFloat() * 25f,
                -35f + r.nextFloat() * 40f,
                r.nextFloat() * 5.5f,
                2.6f + r.nextFloat() * 1.6f
            )
        }
    }
    if (t >= 0f) {
        Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            val w = size.width
            val h = size.height
            val a = min(min(t / 1.4f, (SS_DUR - t) / 1.6f), 1f).coerceIn(0f, 1f)
            // lavender night sky
            drawRect(
                Brush.verticalGradient(
                    0f to Color(0xFFD9D0F5),
                    0.2f to Color(0xFFA79DE6),
                    0.36f to Color(0xFF4A3FA8),
                    0.52f to Color(0xFF211B63),
                    0.75f to Color(0xFF0E0B2E),
                    1f to Color(0xFF06050F)
                ),
                size = Size(w, h * 0.9f),
                alpha = 0.84f * a
            )
            val full = Size(SS_W, SS_H)
            scale(w / SS_W, h / SS_H, Offset.Zero) {
                // tiny stars
                for (st in stars) {
                    val tw = 0.3f + 0.7f * abs(sin(t * 1.7f + st[2]))
                    drawCircle(Color.White.copy(alpha = (0.9f * tw * a).coerceIn(0f, 1f)), st[3], Offset(st[0], st[1]))
                }
                // orange glow on the right
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFFD9591B).copy(alpha = 0.65f * a), Color.Transparent),
                        center = Offset(1060f, 1150f),
                        radius = 560f
                    ),
                    size = full
                )
                // pines: far row, then the front ones with orange rim light
                ssPine(330f, 980f, 1450f, 100f, Color(0xFF17134D).copy(alpha = 0.9f * a), false, t, a)
                ssPine(760f, 960f, 1450f, 105f, Color(0xFF17134D).copy(alpha = 0.9f * a), false, t, a)
                ssPine(560f, 880f, 1480f, 125f, Color(0xFF0B0922).copy(alpha = 0.95f * a), true, t, a)
                ssPine(470f, 760f, 1420f, 150f, Color(0xFF0A0820).copy(alpha = 0.97f * a), true, t, a)
                ssPine(640f, 700f, 1400f, 140f, Color(0xFF09071C).copy(alpha = 0.97f * a), true, t, a)
                // dark frame around the edges
                drawRect(
                    Brush.radialGradient(
                        0.5f to Color.Transparent,
                        1f to Color(0xFF05040F).copy(alpha = 0.92f * a),
                        center = Offset(500f, 700f),
                        radius = 860f
                    ),
                    size = full
                )
                // leaves and branches, swaying a little
                for (lf in leaves) {
                    val dx = sin(t * 0.8f + lf.phase) * 5f
                    val dy = cos(t * 0.6f + lf.phase) * 4f
                    translate(dx, dy) {
                        drawPath(lf.path, (if (lf.dark) Color(0xFF07051A) else Color(0xFF14102E)).copy(alpha = 0.96f * a))
                    }
                }
                // glowing cream sparks
                for (i in sparks.indices) {
                    val sp = sparks[i]
                    val u = (t - sp[7]) / sp[8]
                    if (u >= 0f && u <= 1f) {
                        val x = sp[0] + sp[5] * (t - sp[7])
                        val y = sp[1] + sp[6] * (t - sp[7])
                        val al = sin(PI.toFloat() * u) * a * (0.8f + 0.2f * sin(t * 9f + i))
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Color(0xFFFFC76B).copy(alpha = (0.45f * al).coerceIn(0f, 1f)), Color.Transparent),
                                center = Offset(x, y),
                                radius = sp[2] * 0.9f
                            ),
                            radius = sp[2] * 0.9f,
                            center = Offset(x, y)
                        )
                        rotate(sp[4] + 25f * sin(t * 1.3f + i), Offset(x, y)) {
                            drawOval(
                                Color(0xFFFFF0B8).copy(alpha = (0.95f * al).coerceIn(0f, 1f)),
                                Offset(x - sp[2] / 2f, y - sp[3] / 2f),
                                Size(sp[2], sp[3])
                            )
                        }
                    }
                }
            }
            // let the bottom of the screen fade out so Home stays usable
            drawRect(
                Brush.verticalGradient(0f to Color.Black, 0.62f to Color.Black, 0.9f to Color.Transparent),
                blendMode = BlendMode.DstIn
            )
        }
    }
}
