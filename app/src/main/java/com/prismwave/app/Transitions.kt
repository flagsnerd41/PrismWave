package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Settings for the cover animation between songs.
object TransitionPrefs {
    private var p: SharedPreferences? = null
    var blur by mutableStateOf(true)
    var blurSecs by mutableStateOf(6)
    var shuffle by mutableStateOf(true)

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_transitions", Context.MODE_PRIVATE)
        p = sp
        blur = sp.getBoolean("blur", true)
        blurSecs = sp.getInt("blurSecs", 6).coerceIn(3, 12)
        shuffle = sp.getBoolean("shuffle", true)
    }

    fun save() {
        p?.edit()
            ?.putBoolean("blur", blur)
            ?.putInt("blurSecs", blurSecs)
            ?.putBoolean("shuffle", shuffle)
            ?.apply()
    }
}

// The big cover in the player.
// 1. The last seconds of a song blur the cover slowly (same length as the audio blur-out).
// 2. When it is super blurry the cover flicks through random covers from your music.
// 3. It lands on the next song's cover and slowly clears, like the muffled audio clears.
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
    var busy by remember { mutableStateOf(false) }
    val post = remember { Animatable(0f) }
    val lastBlur = remember { floatArrayOf(0f) }

    val total = song.seconds.coerceAtLeast(1).toFloat()
    val left = total - s.position
    val secs = (if (AutoMix.on) AutoMix.leadFor(song.id) else null)
        ?: (if (StudioPrefs.mixOn) StudioPrefs.mixSecs.toFloat() else TransitionPrefs.blurSecs.toFloat())
    val live = TransitionPrefs.blur && !Opts.lite && !s.reduceMotion
    val target = if (live && !busy && shown.id == song.id && total > secs * 3f && left > 0f && left < secs) {
        ((secs - left) / secs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val pre by animateFloatAsState(
        target,
        tween(if (target == 0f) 350 else 1100, easing = LinearEasing),
        label = "coverBlur"
    )
    if (shown.id == song.id) lastBlur[0] = pre
    val b = if (busy) post.value else pre

    LaunchedEffect(song.id) {
        if (shown.id == song.id || !TransitionPrefs.shuffle || Opts.lite || s.reduceMotion) {
            shown = song
            busy = false
            return@LaunchedEffect
        }
        val natural = lastBlur[0] > 0.45f
        post.snapTo(if (natural) lastBlur[0] else 0f)
        busy = true
        post.animateTo(1f, tween(if (natural) 300 else 260))
        val src = if (s.coverPool.isNotEmpty()) s.coverPool else (s.queue + s.history.toList() + s.liked.toList())
        val pool = src
            .filter { it.id != song.id && it.id != shown.id && it.thumb != null }
            .distinctBy { it.id }
            .shuffled()
            .take(12)
        val steps = if (natural) pool.size.coerceAtMost(10) else pool.size.coerceAtMost(5)
        for (i in 0 until steps) {
            shown = pool[i]
            delay(65L + i * 28L)
        }
        shown = song
        val clearMs = if (natural) {
            if (StudioPrefs.mixOn) (StudioPrefs.mixSecs * 1000L).coerceIn(2500L, 8000L) else 3500L
        } else {
            1200L
        }
        post.animateTo(0f, tween(clearMs.toInt(), easing = LinearOutSlowInEasing))
        busy = false
    }
    LaunchedEffect(song) {
        if (shown.id == song.id) shown = song
    }

    Box(modifier.clip(RoundedCornerShape(corner))) {
        Cover(
            shown,
            if (b > 0.01f) Modifier.fillMaxSize().blur((b * 30f).dp) else Modifier.fillMaxSize(),
            0.dp,
            textSize,
            quality = quality
        )
    }
}

// Loads covers into Coil's cache ahead of time so they appear instantly.
object CoverPreload {
    fun warm(ctx: Context, songs: List<Song>, quality: Int) {
        val loader = coil.Coil.imageLoader(ctx)
        for (sg in songs) {
            val t = sg.thumb ?: continue
            loader.enqueue(coil.request.ImageRequest.Builder(ctx).data(artUrl(t, quality)).build())
        }
    }
}
