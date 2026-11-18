package com.prismwave.app

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
 *
 * The helper player is prepared a few seconds BEFORE the fade starts (prepare), so when the fade starts (begin)
 * the next song is audible right away and the two songs really overlap, with no silent gap.
 */
object XFade {
    private var player: ExoPlayer? = null
    private var readyId: String? = null
    var songId: String? = null
        private set
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // the overlap is audible right now
    val active: Boolean get() = player != null && songId != null

    // loaded and waiting silently, not audible yet
    val preparedId: String? get() = if (player != null && songId == null) readyId else null

    private fun build(ctx: Context, url: String): ExoPlayer {
        val p = ExoPlayer.Builder(ctx.applicationContext).build()
        // no audio focus of its own, the main player keeps it
        p.setAudioAttributes(
            AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(),
            false
        )
        p.volume = 0f
        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.playWhenReady = false
        return p
    }

    // loads the next song silently so it can start the moment the crossfade begins
    fun prepare(ctx: Context, id: String, url: String) {
        if (player != null && readyId == id) return
        stop()
        player = build(ctx, url)
        readyId = id
    }

    // starts the next song and fades it in over ms (the time left of the current song)
    fun begin(ctx: Context, id: String, url: String, ms: Long) {
        if (player == null || readyId != id) {
            stop()
            player = build(ctx, url)
            readyId = id
        }
        val p = player ?: return
        songId = id
        job?.cancel()
        p.volume = 0f
        p.playWhenReady = true
        job = scope.launch {
            // the fade-in only starts when the sound is really playing, so it is never finished before it is heard
            var waited = 0L
            while (waited < 5000L && !(p.playbackState == Player.STATE_READY && p.isPlaying)) {
                delay(30)
                waited += 30L
            }
            val total = (ms - waited).coerceAtLeast(300L)
            val steps = (total / 40L).coerceAtLeast(1L)
            for (k in 1..steps) {
                val t = k.toFloat() / steps
                p.volume = kotlin.math.sin(t * Math.PI / 2.0).toFloat()
                delay(40)
            }
            p.volume = 1f
        }
    }

    fun start(ctx: Context, id: String, url: String, ms: Long) = begin(ctx, id, url, ms)

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
        readyId = null
        if (p != null) {
            try {
                p.release()
            } catch (e: Exception) {
            }
        }
    }
}
