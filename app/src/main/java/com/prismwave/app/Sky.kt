package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------- options for the looks

object LookPrefs {
    private var p: SharedPreferences? = null
    var catOn by mutableStateOf(true)
    var skyOn by mutableStateOf(true)
    // only for trying the sky: null = real clock
    var previewHour by mutableStateOf<Float?>(null)

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_looks", Context.MODE_PRIVATE)
        p = sp
        catOn = sp.getBoolean("catOn", true)
        skyOn = sp.getBoolean("skyOn", true)
    }

    fun save() {
        p?.edit()?.putBoolean("catOn", catOn)?.putBoolean("skyOn", skyOn)?.apply()
    }
}

// ---------------------------------------------------------------- the time of day

private fun hourFraction(): Float {
    val c = Calendar.getInstance()
    return c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60f
}

@Composable
fun rememberHour(): Float {
    var h by remember { mutableStateOf(hourFraction()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L)
            h = hourFraction()
        }
    }
    return LookPrefs.previewHour ?: h
}

@Composable
fun skyGreeting(): String {
    val h = rememberHour()
    return when {
        h < 5f || h >= 21f -> "Good night"
        h < 12f -> "Good morning"
        h < 17f -> "Good afternoon"
        else -> "Good evening"
    }
}

private class SkyKey(val h: Float, val core: Color, val halo: Color, val outer: Color, val orbY: Float, val night: Float)

private val SkyKeys = listOf(
    SkyKey(0f, Color(0xFFDCE6FF), Color(0xFF3A3FA8), Color(0xFF1B1F6B), 0.16f, 1f),
    SkyKey(4.5f, Color(0xFFDCE6FF), Color(0xFF3A3FA8), Color(0xFF1B1F6B), 0.16f, 1f),
    SkyKey(6f, Color(0xFFFFB067), Color(0xFFFF6A5C), Color(0xFF7B3FB0), 0.30f, 0.35f),
    SkyKey(8f, Color(0xFFFF9A3D), Color(0xFFFF7A45), Color(0xFFE0568A), 0.20f, 0f),
    SkyKey(11f, Color(0xFFFFD25A), Color(0xFFFFB347), Color(0xFF4FA3FF), 0.14f, 0f),
    SkyKey(14f, Color(0xFFFFE27A), Color(0xFFFFC94A), Color(0xFF4FA3FF), 0.12f, 0f),
    SkyKey(17f, Color(0xFFFFB347), Color(0xFFFF7A3D), Color(0xFFC4508A), 0.20f, 0f),
    SkyKey(19f, Color(0xFFFF6B3D), Color(0xFFE0407A), Color(0xFF5B3FD6), 0.32f, 0.2f),
    SkyKey(21f, Color(0xFFC9D6FF), Color(0xFF4A4FB8), Color(0xFF241F7A), 0.18f, 0.9f),
    SkyKey(24f, Color(0xFFDCE6FF), Color(0xFF3A3FA8), Color(0xFF1B1F6B), 0.16f, 1f)
)

private class SkyNow(val core: Color, val halo: Color, val outer: Color, val orbY: Float, val night: Float)

private fun skyAt(h: Float): SkyNow {
    var a = SkyKeys.first()
    var b = SkyKeys.last()
    for (i in 0 until SkyKeys.size - 1) {
        if (h >= SkyKeys[i].h && h <= SkyKeys[i + 1].h) {
            a = SkyKeys[i]
            b = SkyKeys[i + 1]
            break
        }
    }
    val f = if (b.h > a.h) ((h - a.h) / (b.h - a.h)).coerceIn(0f, 1f) else 0f
    return SkyNow(
        lerp(a.core, b.core, f),
        lerp(a.halo, b.halo, f),
        lerp(a.outer, b.outer, f),
        a.orbY + (b.orbY - a.orbY) * f,
        a.night + (b.night - a.night) * f
    )
}

// ---------------------------------------------------------------- sun, sunset and moon glow for Home

@Composable
fun HomeTopGlow(s: AppState, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    remember { LookPrefs.init(ctx); 0 }
    if (LookPrefs.skyOn) {
        SkyGlow(s, modifier)
    } else {
        Box(
            modifier
                .fillMaxWidth()
                .height(430.dp)
                .background(
                    Brush.verticalGradient(
                        0f to s.accent.copy(alpha = 0.30f),
                        0.42f to s.accent.copy(alpha = 0.12f),
                        1f to Color.Transparent
                    )
                )
        )
    }
}

@Composable
fun SkyGlow(s: AppState, modifier: Modifier = Modifier) {
    val hour = rememberHour()
    val sky = skyAt(hour)
    val still = s.reduceMotion || s.batterySaver || Opts.lite
    val tr = rememberInfiniteTransition(label = "sky")
    val ph by tr.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "skyPh"
    )
    val stars = remember {
        val r = Random(21)
        List(34) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.5f, r.nextFloat() * 6.28f, 0.8f + r.nextFloat() * 1.4f) }
    }
    val tint = s.accent
    val cur = s.current
    val cover = if (cur != null) rememberArtColor(cur) else tint
    val coverA by androidx.compose.animation.animateColorAsState(cover, tween(900), label = "glowc")
    Box(
        modifier
            .fillMaxWidth()
            .height(520.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Black, 0.3f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val p = if (still) 0f else ph
            val w = size.width
            val hh = size.height
            val ang = p * 2f * PI.toFloat()
            val c = Offset(w * 0.08f + sin(ang) * w * 0.015f, hh * sky.orbY * 0.45f + cos(ang) * hh * 0.006f)
            val night = sky.night
            val sunV = (1f - night).coerceIn(0f, 1f)
            val d = 0.45f + 0.55f * sunV
            val c1 = coverA
            val c2 = lerp(coverA, sky.halo, 0.45f)
            val c3 = lerp(coverA, Color.White, 0.18f)
            val b1 = Offset(w * (0.15f + 0.10f * sin(ang)), hh * 0.10f)
            val b2 = Offset(w * (0.60f + 0.12f * cos(ang + 1f)), hh * 0.05f)
            val b3 = Offset(w * (0.98f + 0.05f * sin(ang + 2f)), hh * 0.24f)
            drawRect(Brush.radialGradient(listOf(c1.copy(alpha = 0.70f * d), Color.Transparent), center = b1, radius = w * 0.9f))
            drawRect(Brush.radialGradient(listOf(c2.copy(alpha = 0.55f * d), Color.Transparent), center = b2, radius = w * 0.75f))
            drawRect(Brush.radialGradient(listOf(c3.copy(alpha = 0.40f * d), Color.Transparent), center = b3, radius = w * 0.6f))
            if (night > 0.03f) {
                drawRect(Brush.radialGradient(listOf(sky.core.copy(alpha = 0.45f * night), Color.Transparent), center = c, radius = w * 0.34f))
                drawCircle(sky.core.copy(alpha = 0.9f * night), radius = w * 0.06f, center = c)
                drawCircle(Color(0x1A000000), radius = w * 0.018f, center = Offset(c.x - w * 0.02f, c.y - w * 0.015f))
                drawCircle(Color(0x16000000), radius = w * 0.012f, center = Offset(c.x + w * 0.025f, c.y + w * 0.02f))
                drawCircle(Color(0x14000000), radius = w * 0.009f, center = Offset(c.x + w * 0.02f, c.y - w * 0.03f))
                for (st in stars) {
                    val tw = 0.4f + 0.6f * abs(sin(ang * 3f + st[2]))
                    drawCircle(
                        Color.White.copy(alpha = (0.85f * tw * night).coerceIn(0f, 1f)),
                        radius = st[3] * 1.3f,
                        center = Offset(st[0] * w, st[1] * hh)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- the cat that cleans itself on the bottom bar

@Composable
fun NavCat(s: AppState) {
    val ctx = LocalContext.current
    remember { LookPrefs.init(ctx); 0 }
    if (LookPrefs.catOn) {
        val still = s.reduceMotion || s.batterySaver || Opts.lite
        val tr = rememberInfiniteTransition(label = "cat")
        val anim by tr.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
            label = "catT"
        )
        val accent = s.accent
        Box(
            Modifier
                .padding(start = 30.dp)
                .layout { m, c ->
                    val pl = m.measure(c)
                    val pull = 10.dp.roundToPx()
                    layout(pl.width, pl.height - pull) { pl.place(0, 0) }
                }
                .size(104.dp, 64.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val t = if (still) 0.8f else anim
                drawCat(t, accent)
            }
        }
    }
}

private fun DrawScope.drawCat(t: Float, accent: Color) {
    val k = size.width / 100f
    fun pt(x: Float, y: Float): Offset = Offset(x * k, y * k)
    val body = Color(0xFF2B2940)
    val dark = Color(0xFF1E1C30)
    val light = Color(0xFF3B3856)
    val legC = Color(0xFF34324D)
    val twoPi = 2f * PI.toFloat()
    // one loop: sit, lift the paw, lick it four times, put it down, tail swish, look around
    val raise = when {
        t < 0.08f -> 0f
        t < 0.16f -> (t - 0.08f) / 0.08f
        t < 0.50f -> 1f
        t < 0.58f -> 1f - (t - 0.50f) / 0.08f
        else -> 0f
    }
    val lick = if (t >= 0.16f && t <= 0.50f) sin((t - 0.16f) / 0.34f * twoPi * 4f) else 0f
    val sw = sin(t * twoPi * 2f)
    val hx = 62f + raise * 1.5f
    val hy = 20f + raise * 2f + raise * abs(lick) * 1.2f
    val blink = (t >= 0.80f && t <= 0.83f) || (t >= 0.90f && t <= 0.92f)
    val closed = raise > 0.4f || blink
    val twitch = if (t >= 0.66f && t <= 0.70f) -1.5f else 0f

    // soft shadow on the bar
    drawOval(Color(0x33000000), pt(14f, 58f), Size(64f * k, 4.5f * k))
    // tail
    val tail = Path()
    tail.moveTo(22f * k, 54f * k)
    tail.quadraticBezierTo(2f * k, 52f * k, (9f + 4f * sw) * k, 26f * k)
    drawPath(tail, dark, style = Stroke(width = 6f * k, cap = StrokeCap.Round))
    // far front leg
    drawLine(dark, pt(54f, 40f), pt(55f, 57f), strokeWidth = 7f * k, cap = StrokeCap.Round)
    // body, haunch, chest, back foot
    drawOval(body, pt(18f, 26f), Size(52f * k, 34f * k))
    drawCircle(body, 15f * k, pt(32f, 45f))
    drawOval(body, pt(50f, 22f), Size(24f * k, 38f * k))
    drawOval(light, pt(27f, 54f), Size(22f * k, 6.5f * k))
    drawOval(light, pt(49f, 55f), Size(12f * k, 5.5f * k))
    // ears
    val ear1 = Path()
    ear1.moveTo((hx - 10f) * k, (hy - 4f) * k)
    ear1.lineTo((hx - 8f) * k, (hy - 17f) * k)
    ear1.lineTo((hx - 0.5f) * k, (hy - 10f) * k)
    ear1.close()
    val ear2 = Path()
    ear2.moveTo((hx + 1f) * k, (hy - 10f) * k)
    ear2.lineTo((hx + 8f) * k, (hy - 18f + twitch) * k)
    ear2.lineTo((hx + 11f) * k, (hy - 5f) * k)
    ear2.close()
    drawPath(ear1, body)
    drawPath(ear2, body)
    val inner = accent.copy(alpha = 0.55f)
    val in1 = Path()
    in1.moveTo((hx - 8.5f) * k, (hy - 6.5f) * k)
    in1.lineTo((hx - 7.7f) * k, (hy - 13.5f) * k)
    in1.lineTo((hx - 3.5f) * k, (hy - 9.5f) * k)
    in1.close()
    val in2 = Path()
    in2.moveTo((hx + 2.5f) * k, (hy - 10f) * k)
    in2.lineTo((hx + 7.6f) * k, (hy - 15f + twitch) * k)
    in2.lineTo((hx + 9f) * k, (hy - 7f) * k)
    in2.close()
    drawPath(in1, inner)
    drawPath(in2, inner)
    // head
    drawCircle(body, 12f * k, pt(hx, hy))
    val e1 = pt(hx + 2f, hy - 1f)
    val e2 = pt(hx + 8.5f, hy - 1f)
    if (closed) {
        for (e in listOf(e1, e2)) {
            val arc = Path()
            arc.moveTo(e.x - 2.4f * k, e.y)
            arc.quadraticBezierTo(e.x, e.y + 2.4f * k, e.x + 2.4f * k, e.y)
            drawPath(arc, Color.White.copy(alpha = 0.85f), style = Stroke(width = 1.1f * k, cap = StrokeCap.Round))
        }
    } else {
        val eye = lerp(accent, Color.White, 0.35f)
        for (e in listOf(e1, e2)) {
            drawOval(eye, Offset(e.x - 1.9f * k, e.y - 2.4f * k), Size(3.8f * k, 4.8f * k))
            drawLine(Color(0xFF0B0A14), Offset(e.x, e.y - 2.2f * k), Offset(e.x, e.y + 2.2f * k), strokeWidth = 1f * k, cap = StrokeCap.Round)
        }
    }
    drawCircle(Color(0xFFFF9FB5), 1.1f * k, pt(hx + 11.5f, hy + 2.6f))
    val wh = Color.White.copy(alpha = 0.4f)
    drawLine(wh, pt(hx + 10f, hy + 3f), pt(hx + 19f, hy + 1f), strokeWidth = 0.7f * k)
    drawLine(wh, pt(hx + 10f, hy + 4f), pt(hx + 19f, hy + 6f), strokeWidth = 0.7f * k)
    // the paw it licks
    val shoulder = pt(65f, 37f)
    val rad = (105f - 170f * raise + lick * raise * 9f) * PI.toFloat() / 180f
    val len = (20f - 8f * raise) * k
    val tip = Offset(shoulder.x + cos(rad) * len, shoulder.y + sin(rad) * len)
    drawLine(legC, shoulder, tip, strokeWidth = 7.5f * k, cap = StrokeCap.Round)
    drawCircle(Color(0xFF45426A), 4.4f * k, tip)
    if (raise > 0.9f && lick > 0.2f) {
        drawCircle(Color(0xFFFF8FA8), 1.7f * k, pt(hx + 9.5f, hy + 6.5f))
    }
}
