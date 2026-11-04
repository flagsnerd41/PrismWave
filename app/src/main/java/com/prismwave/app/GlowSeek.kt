package com.prismwave.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

// The song progress bar of the full player: the filled part gets brighter toward its tip,
// and the tip glows softly, like a little light travelling along the bar.
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
    val tr = rememberInfiniteTransition(label = "gseek")
    val pulse by tr.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gseekp"
    )
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
        // empty track
        Box(
            Modifier
                .fillMaxWidth()
                .height(h)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.28f))
        )
        // filled part, brighter toward the tip
        Box(
            Modifier
                .fillMaxWidth()
                .height(h)
                .clip(RoundedCornerShape(50))
                .drawBehind {
                    val frac = (positionProvider() / max).coerceIn(0f, 1f)
                    val w = size.width * frac
                    if (w > 0.5f) {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.95f)),
                                startX = 0f,
                                endX = w
                            ),
                            size = Size(w, size.height)
                        )
                    }
                }
        )
        // the glowing tip, drawn outside the bar so the light can spread
        Canvas(Modifier.matchParentSize()) {
            val frac = (positionProvider() / max).coerceIn(0f, 1f)
            val x = size.width * frac
            val cy = size.height / 2f
            val hp = h.toPx()
            val pz = if (still) 1f else pulse
            val rad = 24.dp.toPx() * (0.85f + 0.15f * pz)
            drawCircle(
                Brush.radialGradient(
                    listOf(glowColor.copy(alpha = 0.55f * pz), Color.Transparent),
                    center = Offset(x, cy),
                    radius = rad
                ),
                radius = rad,
                center = Offset(x, cy)
            )
            val tail = minOf(34.dp.toPx(), x)
            if (tail > 1f) {
                drawRoundRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.95f)),
                        startX = x - tail,
                        endX = x
                    ),
                    topLeft = Offset(x - tail, cy - hp / 2f),
                    size = Size(tail, hp),
                    cornerRadius = CornerRadius(hp / 2f)
                )
            }
            drawCircle(Color.White, radius = hp * 0.62f, center = Offset(x, cy))
        }
    }
}
