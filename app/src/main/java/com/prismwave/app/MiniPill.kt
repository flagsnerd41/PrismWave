package com.prismwave.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The extra mini player style: one big pill, round cover with a progress ring, bold title and artist,
 * and three round buttons. Colors follow the app theme (Material You), so it matches the rest of the app.
 */
@Composable
fun MiniPillBody(s: AppState, song: Song, scale: Float) {
    val container = ThemeState.scheme?.primaryContainer ?: s.accent
    val on = ThemeState.scheme?.onPrimaryContainer ?: Color.Black
    val frac = (s.position / song.seconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .trackMini()
            .clip(RoundedCornerShape(50))
            .background(container)
            .softClick { s.showPlayer = true }
            .pointerInput(Unit) {
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onDragEnd = { if (dx > 120f) s.prev() else if (dx < -120f) s.next() }
                ) { _, d -> dx += d }
            }
            .pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragStart = { dy = 0f },
                    onDragEnd = { if (dy < -50f) s.showPlayer = true },
                    onDragCancel = { dy = 0f }
                ) { _, d -> dy += d }
            }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // round cover inside a progress ring
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(60.dp)) {
                    val w = 3.dp.toPx()
                    val d = size.minDimension - w
                    val tl = Offset(w / 2f, w / 2f)
                    drawArc(on.copy(alpha = 0.18f), 0f, 360f, false, tl, Size(d, d), style = Stroke(w))
                    drawArc(on.copy(alpha = 0.75f), -90f, 360f * frac, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
                }
                Cover(
                    song,
                    Modifier
                        .size(46.dp)
                        .onGloballyPositioned { MiniCover.rect = it.boundsInRoot() }
                        .graphicsLayer { alpha = if (FlightState.busy) 0f else 1f },
                    23.dp,
                    18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            // the text gets all the space that is left, so it can never run under the buttons
            Column(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = song.title,
                    transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
                    label = "pillTitle"
                ) { t ->
                    Text(t, color = on, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                AnimatedContent(
                    targetState = if (s.status.isNotEmpty()) s.status else song.artist,
                    transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
                    label = "pillArtist"
                ) { t ->
                    Text(t, color = on.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            PillButton(Icons.Rounded.SkipPrevious, on, container, 42.dp) { s.prev() }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.bounceClick { s.toggle() }.size(42.dp).clip(CircleShape).background(on.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                if (s.loading) {
                    CircularProgressIndicator(color = on, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = on, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(6.dp))
            PillButton(Icons.Rounded.SkipNext, on, container, 42.dp) { s.next() }
        }
    }
}

@Composable
private fun PillButton(icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, fg: Color, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        Modifier.bounceClick(onClick).size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
    }
}
