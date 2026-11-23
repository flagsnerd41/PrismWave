package com.prismwave.app

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ------------------------------------------------------------ progress bar styles

// 0 = Wave, 1 = Material 3, 2 = Line, 3 = Pill, 4 = Dotted, 5 = Apple Music
object SeekPrefs {
    var style by mutableStateOf(0)
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        val sp = ctx.getSharedPreferences("prismwave", 0)
        // everyone moves once to the Material 3 progress bar, after that the choice is theirs
        if (!sp.getBoolean("seekM3", false)) {
            sp.edit().putInt("seekStyle", 1).putBoolean("seekM3", true).apply()
        }
        style = sp.getInt("seekStyle", 1)
    }

    fun set(ctx: Context, v: Int) {
        style = v
        ctx.getSharedPreferences("prismwave", 0).edit().putInt("seekStyle", v).apply()
    }
}

// Account options
object AccountPrefs {
    var autoSync by mutableStateOf(true)
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        autoSync = ctx.getSharedPreferences("prismwave", 0).getBoolean("autoSync", true)
    }

    fun set(ctx: Context, v: Boolean) {
        autoSync = v
        ctx.getSharedPreferences("prismwave", 0).edit().putBoolean("autoSync", v).apply()
    }
}

// the progress bar of the player, in the style chosen in Settings, with the stream info centered above it
@Composable
fun StyledSeekBar(s: AppState, total: Float, color: Color, modifier: Modifier = Modifier) {
    val song = s.current
    Column(modifier.fillMaxWidth().controlGlow(color), horizontalAlignment = Alignment.CenterHorizontally) {
        if (song != null && SeekPrefs.style != 5) StreamChip(song)
        val onSeek: (Float) -> Unit = { s.seekTo(it) }
        when (SeekPrefs.style) {
            1 -> M3Seek({ s.position }, total, onSeek, color)
            2 -> CanvasSeek({ s.position }, total, onSeek, 18.dp) { frac, drag ->
                val cy = size.height / 2f
                val st = 3.dp.toPx()
                drawLine(color.copy(alpha = 0.25f), Offset(0f, cy), Offset(size.width, cy), st, StrokeCap.Round)
                drawLine(color, Offset(0f, cy), Offset(size.width * frac, cy), st, StrokeCap.Round)
                drawCircle(color, if (drag) 8.dp.toPx() else 5.dp.toPx(), Offset(size.width * frac, cy))
            }
            3 -> CanvasSeek({ s.position }, total, onSeek, 26.dp) { frac, drag ->
                val h = if (drag) 16.dp.toPx() else 12.dp.toPx()
                val top = (size.height - h) / 2f
                drawRoundRect(color.copy(alpha = 0.22f), Offset(0f, top), Size(size.width, h), CornerRadius(h / 2f))
                val w = (size.width * frac).coerceAtLeast(h)
                drawRoundRect(color, Offset(0f, top), Size(w, h), CornerRadius(h / 2f))
            }
            4 -> CanvasSeek({ s.position }, total, onSeek, 26.dp) { frac, drag ->
                val gap = 10.dp.toPx()
                val n = (size.width / gap).toInt().coerceAtLeast(2)
                val cy = size.height / 2f
                for (i in 0..n) {
                    val x = size.width * i / n
                    val on = x <= size.width * frac
                    val r = if (on) 2.6.dp.toPx() else 1.8.dp.toPx()
                    drawCircle(if (on) color else color.copy(alpha = 0.3f), r, Offset(x, cy))
                }
                drawCircle(color, if (drag) 9.dp.toPx() else 6.dp.toPx(), Offset(size.width * frac, cy))
            }
            5 -> AppleSeek(s, total, if (song != null) rememberArtColor(song) else color)
            else -> WaveSeekBar({ s.position }, total, onSeek, color, Modifier.fillMaxWidth(), s.playing)
        }
    }
}

@Composable
private fun M3Seek(position: () -> Float, max: Float, onSeek: (Float) -> Unit, color: Color) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val frac = (position() / max.coerceAtLeast(1f)).coerceIn(0f, 1f)
    Slider(
        value = drag ?: frac,
        onValueChange = { drag = it },
        onValueChangeFinished = {
            drag?.let { onSeek(it * max) }
            drag = null
        },
        colors = SliderDefaults.colors(
            thumbColor = color,
            activeTrackColor = color,
            inactiveTrackColor = color.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CanvasSeek(
    position: () -> Float,
    max: Float,
    onSeek: (Float) -> Unit,
    height: Dp,
    draw: DrawScope.(Float, Boolean) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
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
            draw((position() / max.coerceAtLeast(1f)).coerceIn(0f, 1f), dragging)
        }
    }
}

// what the stream really is, centered above the progress bar
@Composable
fun StreamChip(song: Song) {
    val t = StreamMeta.byUrl[song.url] ?: return
    Text(
        t,
        color = Color.White.copy(alpha = 0.8f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

// ------------------------------------------------------------ loading circle (Android 17 style): a soft shape that morphs and turns

@Composable
fun PrismLoader(modifier: Modifier = Modifier, size: Dp = 64.dp, color: Color = PrismBlue) {
    val tr = rememberInfiniteTransition(label = "loader")
    val turn by tr.animateFloat(
        0f, 360f, infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart), label = "lt"
    )
    val morph by tr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse), label = "lm"
    )
    val hue by tr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Reverse), label = "lh"
    )
    val c2 = androidx.compose.ui.graphics.lerp(color, Color(0xFFF2B8E0), 0.75f)
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val base = this.size.minDimension * 0.36f
        val amp = 0.03f + 0.15f * morph
        val lobes = 7
        val rot = (turn * PI / 180.0).toFloat()
        val p = Path()
        val steps = 120
        for (i in 0..steps) {
            val a = (i.toFloat() / steps) * (2f * PI.toFloat())
            val r = base * (1f + amp * cos(lobes * a + rot * 2f))
            val x = cx + r * cos(a + rot)
            val y = cy + r * sin(a + rot)
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        drawPath(p, androidx.compose.ui.graphics.lerp(color, c2, hue))
    }
}

@Composable
fun LoaderBox(height: Dp = 220.dp) {
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) { PrismLoader() }
}
