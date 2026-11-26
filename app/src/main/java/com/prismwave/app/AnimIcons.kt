package com.prismwave.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Live menu icons, drawn by hand: the chosen one pops in and keeps moving.
 * kind 0 = Home (the roof lifts and the door swings open), 1 = Library (equalizer bars dance),
 * 2 = Settings (the gear turns). Icons that are not chosen stay still, so they cost no battery.
 */
@Composable
fun LiveNavIcon(kind: Int, selected: Boolean, color: Color, size: Dp = 27.dp) {
    val fill by animateFloatAsState(if (selected) 1f else 0f, tween(320), label = "liveFill")
    val t = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            pop.snapTo(0.70f)
            pop.animateTo(1f, spring(dampingRatio = 0.36f, stiffness = 360f))
        }
    }
    LaunchedEffect(selected) {
        if (selected) {
            val dur = if (kind == 2) 6000 else 3200
            while (true) {
                val remain = ((1f - t.value) * dur).toInt().coerceAtLeast(1)
                t.animateTo(1f, tween(remain, easing = LinearEasing))
                t.snapTo(0f)
            }
        }
    }
    Canvas(
        Modifier.size(size).graphicsLayer {
            scaleX = pop.value
            scaleY = pop.value
        }.liquidGlyph(
            if (selected) 0.3.dp else 1.dp,
            if (selected) Color(0xFFFA243C) else null
        )
    ) {
        val w = this.size.width
        val m = fill
        val tv = t.value
        val sw = w * 0.085f
        val line = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val soft = color.copy(alpha = 0.30f * m)
        when (kind) {
            0 -> {
                val lift = w * 0.045f * m * max(0f, sin(tv * 2f * PI.toFloat()))
                val house = Path().apply {
                    moveTo(0.08f * w, 0.50f * w)
                    lineTo(0.50f * w, 0.12f * w - lift)
                    lineTo(0.92f * w, 0.50f * w)
                    lineTo(0.80f * w, 0.50f * w)
                    lineTo(0.80f * w, 0.80f * w)
                    quadraticBezierTo(0.80f * w, 0.88f * w, 0.72f * w, 0.88f * w)
                    lineTo(0.28f * w, 0.88f * w)
                    quadraticBezierTo(0.20f * w, 0.88f * w, 0.20f * w, 0.80f * w)
                    lineTo(0.20f * w, 0.50f * w)
                    close()
                }
                drawPath(house, soft, style = Fill)
                drawPath(house, color, style = line)
                // the door swings open and shut
                val open = m * sin(min(1f, tv / 0.5f) * PI.toFloat())
                val dw = 0.20f * w * (1f - 0.65f * open)
                drawRoundRect(
                    color,
                    topLeft = Offset(0.5f * w - dw / 2f, 0.62f * w),
                    size = Size(dw, 0.26f * w),
                    cornerRadius = CornerRadius(0.04f * w),
                    style = Stroke(width = sw * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            1 -> {
                drawRoundRect(
                    soft,
                    topLeft = Offset(0.12f * w, 0.12f * w),
                    size = Size(0.76f * w, 0.76f * w),
                    cornerRadius = CornerRadius(0.22f * w)
                )
                drawRoundRect(
                    color,
                    topLeft = Offset(0.12f * w, 0.12f * w),
                    size = Size(0.76f * w, 0.76f * w),
                    cornerRadius = CornerRadius(0.22f * w),
                    style = line
                )
                val xs = floatArrayOf(0.34f, 0.50f, 0.66f)
                val base = floatArrayOf(0.20f, 0.34f, 0.26f)
                val ks = floatArrayOf(1f, 2f, 3f)
                val ph = floatArrayOf(0f, 2f, 4f)
                for (i in 0..2) {
                    val h = (base[i] * (1f + m * 0.60f * sin(2f * PI.toFloat() * ks[i] * tv + ph[i]))) * w
                    drawLine(
                        color,
                        start = Offset(xs[i] * w, 0.70f * w),
                        end = Offset(xs[i] * w, 0.70f * w - h),
                        strokeWidth = sw * 1.15f,
                        cap = StrokeCap.Round
                    )
                }
            }
            else -> {
                val c = Offset(0.5f * w, 0.5f * w)
                val deg = 360f * tv * m
                rotate(deg, pivot = c) {
                    for (i in 0 until 8) {
                        val a = (i * 45f) * PI.toFloat() / 180f
                        drawLine(
                            color,
                            start = Offset(c.x + cos(a) * 0.31f * w, c.y + sin(a) * 0.31f * w),
                            end = Offset(c.x + cos(a) * 0.43f * w, c.y + sin(a) * 0.43f * w),
                            strokeWidth = sw * 1.5f,
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(soft, radius = 0.27f * w, center = c)
                    drawCircle(color, radius = 0.27f * w, center = c, style = line)
                    drawCircle(color, radius = 0.095f * w, center = c, style = line)
                }
            }
        }
    }
}
