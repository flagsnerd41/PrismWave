package com.prismwave.app

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Real crossfade: while the end of a song is fading out, the next song already plays on a second player
 * and fades in at the same time, so the two overlap (like Apple Music). When the first song ends, the main player
 * takes the next song over at the same spot and this helper player disappears.
 */
object XFade {
    private var player: ExoPlayer? = null
    var songId: String? = null
        private set
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val active: Boolean get() = player != null && songId != null

    fun start(ctx: Context, id: String, url: String, ms: Long) {
        stop()
        val p = ExoPlayer.Builder(ctx.applicationContext).build()
        // no audio focus of its own, the main player keeps it
        p.setAudioAttributes(
            AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(),
            false
        )
        p.volume = 0f
        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.playWhenReady = true
        player = p
        songId = id
        job = scope.launch {
            val steps = (ms / 40L).coerceAtLeast(1L)
            for (k in 1..steps) {
                val t = k.toFloat() / steps
                p.volume = kotlin.math.sin(t * Math.PI / 2.0).toFloat()
                delay(40)
            }
            p.volume = 1f
        }
    }

    fun position(): Long = player?.currentPosition ?: 0L

    fun fadeOutAndStop(ms: Long) {
        val p = player ?: return
        job?.cancel()
        val startVol = p.volume
        job = scope.launch {
            val steps = (ms / 30L).coerceAtLeast(1L)
            for (k in 1..steps) {
                p.volume = startVol * (1f - k.toFloat() / steps)
                delay(30)
            }
            stop()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        val p = player
        player = null
        songId = null
        if (p != null) {
            try {
                p.release()
            } catch (e: Exception) {
            }
        }
    }
}
