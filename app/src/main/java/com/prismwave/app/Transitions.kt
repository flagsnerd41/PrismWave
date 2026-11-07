package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

// Settings for the cover animations between songs.
object TransitionPrefs {
    private var p: SharedPreferences? = null
    var blur by mutableStateOf(true)
    var blurSecs by mutableStateOf(6)
    var flip by mutableStateOf(true)

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_transitions", Context.MODE_PRIVATE)
        p = sp
        blur = sp.getBoolean("blur", true)
        blurSecs = sp.getInt("blurSecs", 6).coerceIn(3, 12)
        flip = sp.getBoolean("flip", true)
    }

    fun save() {
        p?.edit()
            ?.putBoolean("blur", blur)
            ?.putInt("blurSecs", blurSecs)
            ?.putBoolean("flip", flip)
            ?.apply()
    }
}

// The big cover in the player. The last seconds of a song blur it out slowly,
// and when the song changes the cover square flips over to the new cover.
@Composable
fun TransitionCover(
    s: AppState,
    song: Song,
    modifier: Modifier,
    corner: Dp,
    textSize: TextUnit,
    quality: Int
) {
    TransitionPrefs.init(LocalContext.current.applicationContext)
    var shown by remember { mutableStateOf(song) }
    val rot = remember { Animatable(0f) }
    val density = LocalDensity.current.density

    LaunchedEffect(song.id) {
        if (shown.id == song.id || !TransitionPrefs.flip || Opts.lite || s.reduceMotion) {
            shown = song
        } else {
            rot.animateTo(90f, tween(170, easing = FastOutLinearInEasing))
            shown = song
            rot.snapTo(-90f)
            rot.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 220f))
        }
    }
    LaunchedEffect(song) {
        if (shown.id == song.id) shown = song
    }

    val total = song.seconds.coerceAtLeast(1).toFloat()
    val left = total - s.position
    val secs = TransitionPrefs.blurSecs.toFloat()
    val target = if (TransitionPrefs.blur && !Opts.lite && total > secs * 3f && left > 0f && left < secs) {
        ((secs - left) / secs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val b by animateFloatAsState(
        target,
        tween(if (target == 0f) 350 else 1100, easing = LinearEasing),
        label = "coverBlur"
    )

    Box(
        modifier
            .graphicsLayer {
                rotationY = rot.value
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(corner))
    ) {
        Cover(
            shown,
            if (b > 0.01f) Modifier.fillMaxSize().blur((b * 22f).dp) else Modifier.fillMaxSize(),
            0.dp,
            textSize,
            quality = quality
        )
    }
}
