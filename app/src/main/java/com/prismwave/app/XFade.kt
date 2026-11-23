package com.prismwave.app

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Crossfade, Apple Music style: when the crossfade window starts, the main player jumps to the NEXT song right away
 * (it starts at 0:00 with the same speed and fades in), while this helper player keeps playing the END of the old song
 * from the same spot and fades it out softly. The next song is never handed over, so speed changes can not make it jump.
 *
 * The helper is prepared a few seconds BEFORE the window (prepare), so it can start the moment the window opens.
 */
object XFade {
    private var player: ExoPlayer? = null
    private var readyId: String? = null

    // the id of the OLD song that is fading out right now
    var songId: String? = null
        private set
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // the old song is audible on the helper right now
    val active: Boolean get() = player != null && songId != null

    // loaded and waiting silently, not audible yet
    val preparedId: String? get() = if (player != null && songId == null) readyId else null

    private fun build(ctx: Context, url: String, posMs: Long): ExoPlayer {
        val p = ExoPlayer.Builder(ctx.applicationContext).build()
        // no audio focus of its own, the main player keeps it
        p.setAudioAttributes(
            AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(),
            false
        )
        p.volume = 0f
        p.setMediaItem(MediaItem.fromUri(url), posMs.coerceAtLeast(0L))
        p.prepare()
        p.playWhenReady = false
        return p
    }

    // loads the end of the current song silently so it can take over the moment the crossfade window opens
    fun prepare(ctx: Context, id: String, url: String, posMs: Long) {
        if (player != null && readyId == id) return
        stop()
        player = build(ctx, url, posMs)
        readyId = id
    }

    /**
     * Starts the old song on the helper at the spot the main player has reached (same speed and pitch), then calls
     * switchNow() at the same moment so the main player jumps to the next song, and fades the old song out over fadeMs.
     */
    suspend fun begin(
        ctx: Context,
        id: String,
        url: String,
        mainPos: () -> Long,
        tempo: Float,
        pitch: Float,
        fadeMs: Long,
        switchNow: () -> Unit
    ) {
        if (player == null || readyId != id) {
            stop()
            player = build(ctx, url, mainPos())
            readyId = id
        }
        val p = player ?: return
        job?.cancel()
        p.volume = 0f
        p.playbackParameters = PlaybackParameters(tempo, pitch)
        p.seekTo(mainPos() + 60L)
        p.playWhenReady = true
        // wait (silently) until the helper really plays, never longer than a moment
        var waited = 0L
        while (waited < 900L && !(p.playbackState == Player.STATE_READY && p.isPlaying)) {
            delay(20)
            waited += 20L
        }
        // catch up with the main player if it moved on while the helper was loading
        val diff = mainPos() - p.currentPosition
        if (diff > 40L && diff < 2500L) {
            p.seekTo(p.currentPosition + diff + 50L)
            delay(60)
        }
        songId = id
        p.volume = 1f
        switchNow()
        val steps = (fadeMs / 30L).coerceAtLeast(1L)
        job = scope.launch {
            for (k in 1..steps) {
                val t = k.toFloat() / steps
                // equal-power curve: soft at the start, no dip in the middle
                p.volume = kotlin.math.cos(t * Math.PI / 2.0).toFloat()
                delay(30)
            }
            stop()
        }
    }

    fun position(): Long = player?.currentPosition ?: 0L

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
