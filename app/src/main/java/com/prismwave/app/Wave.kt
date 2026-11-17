package com.prismwave.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** a seek bar whose played part is a moving wave (it calms down when paused or dragged) */
@Composable
fun WaveSeekBar(
    positionProvider: () -> Float,
    max: Float,
    onSeek: (Float) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    playing: Boolean = true
) {
    var dragging by remember { mutableStateOf(false) }
    val amp by animateFloatAsState(if (playing && !dragging) 1f else 0f, tween(500), label = "waveAmp")
    val inf = rememberInfiniteTransition(label = "wave")
    val phase by inf.animateFloat(
        0f, (2.0 * PI).toFloat(),
        infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "wavePhase"
    )
    Box(
        modifier
            .height(36.dp)
            .pointerInput(max) {
                detectTapGestures { off -> onSeek((off.x / size.width).coerceIn(0f, 1f) * max) }
            }
            .pointerInput(max) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                ) { change, _ ->
                    change.consume()
                    onSeek((change.position.x / size.width).coerceIn(0f, 1f) * max)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val frac = (positionProvider() / max).coerceIn(0f, 1f)
            val w = size.width
            val cy = size.height / 2f
            val x = w * frac
            val stroke = 4.dp.toPx()
            val a = 6.dp.toPx() * amp
            val len = 28.dp.toPx()
            val k = (2.0 * PI).toFloat() / len
            drawLine(color.copy(alpha = 0.28f), Offset(x, cy), Offset(w, cy), stroke, StrokeCap.Round)
            val path = Path()
            path.moveTo(0f, cy)
            var px = 0f
            while (px <= x) {
                path.lineTo(px, cy + a * kotlin.math.sin((px * k - phase).toDouble()).toFloat())
                px += 3f
            }
            drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val r = if (dragging) 10.dp.toPx() else 7.dp.toPx()
            drawCircle(color, r, Offset(x, cy + a * kotlin.math.sin((x * k - phase).toDouble()).toFloat()))
        }
    }
}
