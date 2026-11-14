package com.prismwave.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val Spectrum = listOf(
    Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFD60A), Color(0xFF34C759),
    Color(0xFF30D5FF), Color(0xFF0A84FF), Color(0xFFBF5AF2)
)

private const val RAD = (PI / 180.0).toFloat()

private class PrismFace(val pts: List<Offset>, val depth: Float, val light: Float, val front: Boolean)

private fun lerpO(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

private fun pathOf(pts: List<Offset>): Path {
    val p = Path()
    p.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) p.lineTo(pts[i].x, pts[i].y)
    p.close()
    return p
}

// A glass prism that turns slowly. A white beam goes in and a rainbow comes out.
@Composable
fun PrismSpinner(modifier: Modifier = Modifier, still: Boolean = false) {
    val tr = rememberInfiniteTransition(label = "prism")
    val spin by tr.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "prismSpin"
    )
    Canvas(modifier) {
        drawPrism(if (still) 40f else spin)
    }
}

private fun DrawScope.drawPrism(angleDeg: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.42f
    val cy = h * 0.52f
    val r = h * 0.30f
    val hh = h * 0.34f
    val tilt = 18f * RAD
    val ct = cos(tilt)
    val st = sin(tilt)
    val dist = r * 7f
    fun proj(x: Float, y: Float, z: Float): Offset {
        val yy = y * ct - z * st
        val zz = y * st + z * ct
        val p = dist / (dist - zz)
        return Offset(cx + x * p, cy + yy * p)
    }
    val tops = ArrayList<Offset>()
    val bots = ArrayList<Offset>()
    for (i in 0..2) {
        val a = (angleDeg + i * 120f) * RAD
        val x = r * cos(a)
        val z = r * sin(a)
        tops.add(proj(x, -hh, z))
        bots.add(proj(x, hh, z))
    }

    // the light: a thin white beam comes in and splits into colors on the other side
    val spread = 0.55f + 0.45f * abs(sin(angleDeg * 1.5f * RAD))
    val origin = Offset(cx + r * 0.55f, cy + h * 0.02f)
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.95f)),
            startX = 0f,
            endX = cx - r * 0.5f
        ),
        start = Offset(0f, cy + h * 0.08f),
        end = Offset(cx - r * 0.5f, cy + h * 0.03f),
        strokeWidth = 2.2f
    )
    val band = h * 0.045f * spread
    Spectrum.forEachIndexed { k, c ->
        val endY = origin.y + (k - 3f) * band * 2.2f + h * 0.06f
        val path = Path()
        path.moveTo(origin.x, origin.y)
        path.lineTo(w, endY - band)
        path.lineTo(w, endY + band)
        path.close()
        drawPath(
            path,
            brush = Brush.horizontalGradient(
                listOf(c.copy(alpha = 0.55f + 0.4f * spread), Color.Transparent),
                startX = origin.x,
                endX = w
            )
        )
    }

    // the glass: back faces first, so you can see through to them
    val faces = ArrayList<PrismFace>()
    for (i in 0..2) {
        val j = (i + 1) % 3
        val m = (angleDeg + i * 120f + 60f) * RAD
        val nz = sin(m)
        val nx = cos(m)
        val lam = (nx * -0.45f + nz * 0.85f).coerceAtLeast(0f)
        faces.add(PrismFace(listOf(tops[i], tops[j], bots[j], bots[i]), nz, lam, nz > 0f))
    }
    faces.sortBy { it.depth }
    val glassTop = Color(0xFFE8F4FF)
    val glassBot = Color(0xFF6FA8FF)
    val edge = Stroke(width = 1.5f)

    drawPath(pathOf(bots), Color(0xFF9CC8FF).copy(alpha = 0.10f))
    drawPath(pathOf(bots), Color.White.copy(alpha = 0.25f), style = edge)

    for (f in faces) {
        val path = pathOf(f.pts)
        val minY = f.pts.minOf { it.y }
        val maxY = f.pts.maxOf { it.y }
        val a = if (f.front) 0.14f + 0.26f * f.light else 0.06f
        drawPath(
            path,
            brush = Brush.verticalGradient(
                listOf(glassTop.copy(alpha = a), glassBot.copy(alpha = a * 0.8f)),
                startY = minY,
                endY = maxY
            )
        )
        if (f.front && f.light > 0.2f) {
            // a bright streak that slides across the face as it turns
            val t0 = 0.12f
            val t1 = 0.28f
            val streak = listOf(
                lerpO(f.pts[0], f.pts[1], t0), lerpO(f.pts[0], f.pts[1], t1),
                lerpO(f.pts[3], f.pts[2], t1), lerpO(f.pts[3], f.pts[2], t0)
            )
            drawPath(pathOf(streak), Color.White.copy(alpha = 0.32f * f.light))
        }
        drawPath(path, Color.White.copy(alpha = if (f.front) 0.70f else 0.28f), style = edge)
    }

    drawPath(pathOf(tops), Color(0xFFD6ECFF).copy(alpha = 0.30f))
    drawPath(pathOf(tops), Color.White.copy(alpha = 0.85f), style = Stroke(width = 1.2f))
    // the little spot where the beam hits the glass
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.9f), Color.Transparent),
            center = origin,
            radius = h * 0.10f
        ),
        radius = h * 0.10f,
        center = origin
    )
}
