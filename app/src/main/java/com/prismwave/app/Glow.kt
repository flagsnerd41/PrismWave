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
fun Modifier.glowBehind(art: Color, playing: Boolean, battery: Boolean, topFrac: Float = 0.4f): Modifier = this
