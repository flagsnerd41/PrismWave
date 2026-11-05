package com.prismwave.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

/**
 * Makes the video appear fast: the two video players for the current song are prepared in the
 * background while the full player is open, so tapping "Video" has almost nothing left to load.
 */
object VideoWarm {
    private var id: String? = null
    private var main: ExoPlayer? = null
    private var back: ExoPlayer? = null
    private var used: Boolean? = null

    // only people who already use the video mode get the background loading (it costs a little data)
    fun usedBefore(ctx: Context): Boolean {
        used?.let { return it }
        val v = ctx.getSharedPreferences("prismwave", Context.MODE_PRIVATE).getBoolean("videoUsed", false)
        used = v
        return v
    }

    fun markUsed(ctx: Context) {
        if (used == true) return
        used = true
        ctx.getSharedPreferences("prismwave", Context.MODE_PRIVATE).edit().putBoolean("videoUsed", true).apply()
    }

    // starts playing after only a little video is loaded, instead of waiting for a long buffer
    fun newPlayer(ctx: Context, low: Boolean): ExoPlayer {
        val lc = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2500, if (low) 12000 else 20000, 300, 1000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        return ExoPlayer.Builder(ctx).setLoadControl(lc).build().apply { volume = 0f }
    }

    private fun item(url: String, songId: String): MediaItem =
        MediaItem.Builder().setUri(url).setMediaId(songId).build()

    fun warm(ctx: Context, song: Song, startMs: Long) {
        if (id == song.id && main != null) return
        drop()
        val u = VideoSync.url(song) ?: return
        val l = VideoSync.url(song, true) ?: return
        id = song.id
        main = newPlayer(ctx, false).also {
            it.setMediaItem(item(u, song.id), startMs)
            it.prepare()
        }
        back = newPlayer(ctx, true).also {
            it.setMediaItem(item(l, song.id), startMs)
            it.prepare()
        }
    }

    // the video stage takes over the ready player
    fun take(song: Song, low: Boolean): ExoPlayer? {
        if (id != song.id) return null
        return if (low) {
            val p = back
            back = null
            p
        } else {
            val p = main
            main = null
            p
        }
    }

    fun drop() {
        try { main?.release() } catch (e: Exception) { }
        try { back?.release() } catch (e: Exception) { }
        main = null
        back = null
        id = null
    }
}

@Composable
fun VideoWarmup(s: AppState) {
    PlayerBridgeHook(s)
    UpdateHost(s.accent)
    val ctx = LocalContext.current
    val song = s.current
    val open = s.showPlayer
    LaunchedEffect(song?.id, open, VideoSync.wantVideo, VideoSync.quality) {
        if (song == null || !open || VideoSync.wantVideo) {
            VideoWarm.drop()
            return@LaunchedEffect
        }
        if (!VideoWarm.usedBefore(ctx) || Opts.offline) return@LaunchedEffect
        delay(1200)
        VideoSync.check(song)
        if (VideoSync.has[song.id] == true) {
            VideoWarm.warm(ctx, song, (s.position * 1000).toLong() + 4000L)
        }
    }
}
