package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

// The moving glow behind the player controls and the lyrics: big bright blobs of the cover colors
// and dark blobs between them that drift across the screen, so light and dark flow into each other.
object GlowPrefs {
    var on by mutableStateOf(true)

    // 0 = soft, 1 = normal, 2 = strong
    var intensity by mutableStateOf(2)

    // 0 = slow, 1 = normal, 2 = fast
    var speed by mutableStateOf(1)
    private var p: SharedPreferences? = null

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_glow", Context.MODE_PRIVATE)
        p = sp
        on = sp.getBoolean("on", true)
        intensity = sp.getInt("intensity", 2).coerceIn(0, 2)
        speed = sp.getInt("speed", 1).coerceIn(0, 2)
    }

    fun save() {
        p?.edit()?.putBoolean("on", on)?.putInt("intensity", intensity)?.putInt("speed", speed)?.apply()
    }
}

private fun glowHue(c: Color, deg: Float, minSat: Float, bright: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    hsv[0] = (hsv[0] + deg + 360f) % 360f
    if (hsv[1] < minSat) hsv[1] = minSat
    hsv[2] = bright
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// draws the glow behind the content. topFrac: where the glow starts, as a part of the height (0 = whole screen).
// It moves while music plays and slowly stops when paused.
@Composable
fun Modifier.glowBehind(art: Color, playing: Boolean, battery: Boolean, topFrac: Float = 0.4f): Modifier {
    GlowPrefs.init(LocalContext.current.applicationContext)
    val enabled = GlowPrefs.on && !Opts.lite && !battery
    val artA by animateColorAsState(art, tween(900), label = "glowArt")
    val c1 = remember(artA) { glowHue(artA, 0f, 0.55f, 0.95f) }
    val c2 = remember(artA) { glowHue(artA, 55f, 0.60f, 0.90f) }
    val c3 = remember(artA) { glowHue(artA, -50f, 0.60f, 0.85f) }
    val c4 = remember(artA) { glowHue(artA, 160f, 0.50f, 0.80f) }
    val flowK by animateFloatAsState(if (playing) 1f else 0f, tween(1500), label = "glowRun")
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        var last = 0L
        while (true) {
            if (flowK < 0.005f) {
                // paused: no frames needed
                delay(300)
                last = 0L
                continue
            }
            withFrameNanos { now ->
                if (last != 0L) t += (now - last) / 1_000_000_000f * flowK
                last = now
            }
        }
    }
    return this.drawBehind {
        if (!enabled) return@drawBehind
        val w = size.width
        val h = size.height
        val top = h * topFrac
        val span = h - top
        val per = when (GlowPrefs.speed) {
            0 -> 22f
            2 -> 7f
            else -> 13f
        }
        val a = t / per * 2f * PI.toFloat()
        val k = when (GlowPrefs.intensity) {
            0 -> 0.55f
            1 -> 0.80f
            else -> 1f
        }
        fun blob(cx: Float, cy: Float, r: Float, c: Color, al: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(c.copy(alpha = (al * k).coerceIn(0f, 1f)), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }
        // the bright blobs
        blob(w * (0.15f + 0.45f * sin(a)), top + span * (0.30f + 0.25f * cos(a * 1.3f)), w * (0.85f + 0.15f * sin(a * 0.7f)), c1, 0.75f)
        blob(w * (0.85f + 0.40f * cos(a * 0.9f + 1.5f)), top + span * (0.55f + 0.30f * sin(a * 1.1f + 2f)), w * (0.95f + 0.20f * cos(a * 0.6f)), c2, 0.70f)
        blob(w * (0.45f + 0.50f * sin(a * 0.8f + 3f)), top + span * (0.90f + 0.20f * cos(a + 4f)), w * (1.00f + 0.20f * sin(a * 0.5f + 1f)), c3, 0.65f)
        blob(w * (0.65f + 0.45f * cos(a * 1.2f + 5f)), top + span * (0.15f + 0.25f * sin(a * 0.9f + 1f)), w * 0.70f, c4, 0.45f)
        // the dark blobs between them: this is the moving darkness
        blob(w * (0.50f + 0.60f * sin(a * 1.1f + 2.2f)), top + span * (0.45f + 0.35f * cos(a * 0.8f + 0.5f)), w * (0.75f + 0.15f * cos(a)), Color.Black, 0.85f)
        blob(w * (0.30f + 0.55f * cos(a * 0.7f + 4f)), top + span * (0.75f + 0.25f * sin(a * 1.4f + 3f)), w * 0.70f, Color.Black, 0.75f)
    }
}
