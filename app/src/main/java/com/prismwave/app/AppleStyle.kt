package com.prismwave.app

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime as OBedtime
import androidx.compose.material.icons.outlined.Add as OAdd
import androidx.compose.material.icons.outlined.ChatBubbleOutline as OLyrics
import androidx.compose.material.icons.outlined.Repeat as ORepeat
import androidx.compose.material.icons.outlined.Shuffle as OShuffle
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------ Apple Music look: icon style + colors

/** Settings > Appearance > Player > Icon style. false = PrismWave icons, true = Apple Music icons */
object IconStyle {
    var apple by mutableStateOf(false)
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        apple = ctx.getSharedPreferences("prismwave", 0).getBoolean("iconApple", false)
    }

    fun set(ctx: Context, v: Boolean) {
        apple = v
        ctx.getSharedPreferences("prismwave", 0).edit().putBoolean("iconApple", v).apply()
    }
}

// the warm Apple Music tones, pulled toward the cover color so every cover gets its own glow
private val AppleOrange = Color(0xFFFF8A3D)
private val AppleYellow = Color(0xFFFFE066)

/** soft cover-tinted white, for icons */
fun appleTint(art: Color, amount: Float = 0.3f): Color = lerp(Color.White, lerp(art, AppleOrange, 0.35f), amount)

// ------------------------------------------------------------ progress bar (thick pill, orange to yellow, tiny dot)

@Composable
fun AppleSeek(s: AppState, total: Float, art: Color, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf(false) }
    val max = total.coerceAtLeast(1f)
    val start = lerp(art, AppleOrange, 0.55f)
    val end = lerp(art, AppleYellow, 0.8f)
    Box(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(max) {
                detectTapGestures { off -> s.seekTo((off.x / size.width).coerceIn(0f, 1f) * max) }
            }
            .pointerInput(max) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                ) { change, _ ->
                    change.consume()
                    s.seekTo((change.position.x / size.width).coerceIn(0f, 1f) * max)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val frac = (s.position / max).coerceIn(0f, 1f)
            val h = (if (dragging) 15.dp else 11.dp).toPx()
            val top = (size.height - h) / 2f
            drawRoundRect(Color.White.copy(alpha = 0.2f), Offset(0f, top), Size(size.width, h), CornerRadius(h / 2f))
            val w = (size.width * frac).coerceAtLeast(h)
            drawRoundRect(
                Brush.horizontalGradient(listOf(start, end), 0f, w.coerceAtLeast(1f)),
                Offset(0f, top), Size(w, h), CornerRadius(h / 2f)
            )
            // the tiny dot sits at the end of the fill
            drawCircle(Color.White.copy(alpha = 0.95f), h * 0.2f, Offset((w - h / 2f).coerceAtLeast(h / 2f), size.height / 2f))
        }
    }
}

/** time on the left, the stream badge in the middle (like Hi-Res Lossless), time left on the right */
@Composable
fun AppleTimeLabels(s: AppState, total: Float, art: Color) {
    val pos = s.position.coerceIn(0f, total)
    val badge = s.current?.let { StreamMeta.byUrl[it.url] }
    val c = appleTint(art, 0.45f).copy(alpha = 0.85f)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(fmt(pos.toInt()), color = c, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (badge != null) Text(badge, color = c.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text("-" + fmt((total - pos).toInt()), color = c, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ------------------------------------------------------------ control row: chunky rounded icons

@Composable
private fun AppleIcon(icon: ImageVector, tint: Color, size: Dp, box: Dp, onClick: () -> Unit) {
    Box(Modifier.size(box).bounceClick(onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
fun AppleTransportRow(s: AppState, side: Dp, big: Dp) {
    val song = s.current
    val art = if (song != null) rememberArtColor(song) else Color.White
    val bright = appleTint(art, 0.22f)
    // the rewind button is dimmed while there is nothing before this song
    val first = !s.shuffle && song != null && s.queue.firstOrNull()?.id == song.id
    val dim = bright.copy(alpha = if (first) 0.4f else 1f)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppleIcon(Icons.Rounded.FastRewind, dim, side * 1.2f, side + 30.dp) { s.prev() }
        Box(Modifier.size(big + 22.dp).bounceClick { s.toggle() }, contentAlignment = Alignment.Center) {
            if (s.loading) {
                CircularProgressIndicator(color = bright, strokeWidth = 4.dp, modifier = Modifier.size(big))
            } else {
                Icon(
                    if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = bright,
                    modifier = Modifier.size(big * 1.15f)
                )
            }
        }
        AppleIcon(Icons.Rounded.FastForward, bright, side * 1.2f, side + 30.dp) { s.next() }
    }
}

// ------------------------------------------------------------ bottom icons: soft outlines

@Composable
fun AppleBottomIcons(s: AppState, onAdd: () -> Unit, onSleep: () -> Unit) {
    val song = s.current
    val art = if (song != null) rememberArtColor(song) else Color.White
    val soft = appleTint(art, 0.4f).copy(alpha = 0.7f)
    val on = lerp(AppleYellow, art, 0.2f)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppleIcon(OShuffle, if (s.shuffle) on else soft, 26.dp, 48.dp) { s.shuffle = !s.shuffle }
        AppleIcon(OAdd, soft, 26.dp, 48.dp) { onAdd() }
        AppleIcon(OLyrics, soft, 26.dp, 48.dp) { Pages.show("lyrics") }
        AppleIcon(OBedtime, if (s.sleepLeft > 0) on else soft, 26.dp, 48.dp) { onSleep() }
        AppleIcon(ORepeat, if (s.repeat) on else soft, 26.dp, 48.dp) { s.repeat = !s.repeat }
    }
}

/** the round bordered star and more buttons of the Apple Music title row */
fun Modifier.appleRound(art: Color): Modifier =
    this.clip(CircleShape)
        .background(lerp(art, AppleOrange, 0.3f).copy(alpha = 0.38f))
        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)), CircleShape)
