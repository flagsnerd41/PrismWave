package com.prismwave.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private fun shifted(c: Color, deg: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    hsv[0] = ((hsv[0] + deg) % 360f + 360f) % 360f
    hsv[1] = hsv[1].coerceIn(0.45f, 0.90f)
    hsv[2] = hsv[2].coerceIn(0.50f, 0.95f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Shown while a cover is loading (or when it failed): a soft glow whose colors keep drifting,
 * and in the middle a Material You shape that morphs between a circle, a flower and a clover.
 * Small covers only get the still glow, so lists stay light on the battery.
 */
@Composable
fun CoverPlaceholder(base: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val big = minOf(maxWidth, maxHeight) >= 72.dp
        val tState: State<Float> = if (big) {
            rememberInfiniteTransition(label = "coverGlow").animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
                label = "coverGlowT"
            )
        } else {
            remember { mutableStateOf(0.15f) }
        }
        val primary = ThemeState.scheme?.primary ?: lerp(base, Color.White, 0.55f)
        Canvas(Modifier.fillMaxSize()) {
            val t = tState.value
            val w = size.width
            val h = size.height
            val m = min(w, h)
            val twoPi = (2.0 * PI).toFloat()
            drawRect(lerp(Color.Black, base, 0.35f))
            // glow: three soft blobs that circle around while their hue drifts
            val drift = 50f * sin(twoPi * t)
            for (i in 0..2) {
                val c = shifted(base, drift + (i - 1) * 38f)
                val a = twoPi * (t + i / 3f)
                val center = Offset(w * (0.5f + 0.28f * cos(a + i)), h * (0.5f + 0.28f * sin(a + i * 2f)))
                val r = m * 0.75f
                drawCircle(
                    Brush.radialGradient(listOf(c.copy(alpha = 0.85f), Color.Transparent), center = center, radius = r),
                    radius = r,
                    center = center
                )
            }
            // the loading shape
            val mid = Offset(w / 2f, h / 2f)
            drawCircle(Color.White.copy(alpha = 0.14f), radius = m * 0.30f, center = mid)
            val rot = twoPi * t * 3f
            fun bump(k: Int): Float {
                val x = cos(twoPi * (t * 2f - k / 3f))
                return if (x > 0f) x * x else 0f
            }
            val wa = bump(0)
            val wb = bump(1)
            val wc = bump(2)
            val base0 = m * 0.19f
            val path = Path()
            val n = 96
            for (i in 0..n) {
                val th = twoPi * i / n
                val r = base0 * (1f + 0.10f * wa * cos(5f * th + rot) + 0.12f * wb * cos(7f * th - rot) + 0.17f * wc * cos(4f * th + rot * 0.5f))
                val x = mid.x + r * cos(th)
                val y = mid.y + r * sin(th)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, primary.copy(alpha = 0.92f))
        }
    }
}
