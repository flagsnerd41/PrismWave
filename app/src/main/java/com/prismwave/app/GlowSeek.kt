package com.prismwave.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.sin

private class WaveHold {
    var lastP = -1f
    var lastT = 0L
    var amp = 0f
}

// The song progress bar of the full player. The filled part is a wave that moves while the song plays,
// gets brighter toward its tip and has a soft glow along the line (no round halo).
// When the song is paused, or while you drag, the line is straight.
@Composable
fun GlowSeekBar(
    positionProvider: () -> Float,
    max: Float,
    onSeek: (Float) -> Unit,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    val h by animateDpAsState(if (dragging) 12.dp else 6.dp, spring(dampingRatio = 0.6f, stiffness = 400f), label = "gseekh")
    val tr = rememberInfiniteTransition(label = "gwave")
    val ph by tr.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "gwp"
    )
    val hold = remember { WaveHold() }
    val still = Opts.lite
    Box(
        modifier
            .height(30.dp)
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
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(Modifier.matchParentSize()) {
            val p = positionProvider()
            val now = System.nanoTime()
            if (p != hold.lastP) {
                hold.lastP = p
                hold.lastT = now
            }
            // the position keeps moving while a song plays, so no movement means paused
            val playing = now - hold.lastT < 1_800_000_000L
            val target = if (still || dragging || !playing) 0f else 3.2.dp.toPx()
            hold.amp += (target - hold.amp) * 0.12f
            val phase = ph
            val w = size.width
            val cy = size.height / 2f
            val xEnd = w * (p / max).coerceIn(0f, 1f)
            val sw = h.toPx() * 0.85f
            // the part that is not played yet: a straight line
            drawLine(Color.White.copy(alpha = 0.28f), Offset(xEnd, cy), Offset(w, cy), strokeWidth = sw, cap = StrokeCap.Round)
            if (xEnd > 1f) {
                val path = Path()
                val step = 3.dp.toPx()
                val wl = 30.dp.toPx()
                val a = hold.amp
                path.moveTo(0f, cy + a * sin(-phase))
                var x = 0f
                while (x < xEnd) {
                    x = min(x + step, xEnd)
                    path.lineTo(x, cy + a * sin(6.2831855f * x / wl - phase))
                }
                // soft glow along the line, stronger toward the tip
                drawPath(
                    path,
                    Brush.horizontalGradient(
                        listOf(glowColor.copy(alpha = 0.10f), glowColor.copy(alpha = 0.55f)),
                        startX = 0f,
                        endX = xEnd
                    ),
                    style = Stroke(width = sw * 2.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // the line itself
                drawPath(
                    path,
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.55f), Color.White),
                        startX = 0f,
                        endX = xEnd
                    ),
                    style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}
