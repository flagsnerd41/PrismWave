package com.prismwave.app

import android.os.SystemClock
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.delay

/** Experiment: a muted video-only player kept in sync with the audio player. */
object VideoSync {
    var wantVideo by mutableStateOf(false)
    val has = mutableStateMapOf<String, Boolean>()
    private val urls = HashMap<String, String>()

    suspend fun check(song: Song) {
        if (has.containsKey(song.id) || Opts.offline) return
        try {
            val u = Engine.videoUrl(song.url)
            if (u != null) {
                urls[song.id] = u
                has[song.id] = true
            } else {
                has[song.id] = false
            }
        } catch (e: Exception) {
        }
    }

    fun url(song: Song): String? = urls[song.id]
    fun showVideo(song: Song): Boolean = wantVideo && has[song.id] == true
}

@Composable
fun ModePills() {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f)).padding(4.dp)
    ) {
        listOf("Song" to false, "Video" to true).forEach { (label, v) ->
            val sel = VideoSync.wantVideo == v
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (sel) Color.White else Color.Transparent)
                    .softClick { VideoSync.wantVideo = v }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(label, color = if (sel) Color.Black else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun VideoStage(s: AppState, song: Song, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val player = remember { ExoPlayer.Builder(ctx).build().apply { volume = 0f } }
    var vsize by remember { mutableStateOf(IntSize.Zero) }
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                vsize = IntSize(videoSize.width, videoSize.height)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) ready = true
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        }
        player.addListener(l)
        onDispose {
            player.removeListener(l)
            player.release()
        }
    }
    LaunchedEffect(song.id) {
        ready = false
        failed = false
        val url = VideoSync.url(song)
        if (url == null) {
            failed = true
            return@LaunchedEffect
        }
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.seekTo((s.position * 1000).toLong())
        var lastPos = -1f
        var lastAt = 0L
        var lastSeek = 0L
        while (true) {
            val now = SystemClock.elapsedRealtime()
            if (s.position != lastPos) {
                lastPos = s.position
                lastAt = now
            }
            val est = lastPos + (if (s.playing) (now - lastAt) / 1000f else 0f)
            player.playWhenReady = s.playing && !s.loading
            if (player.playbackState != Player.STATE_IDLE && now - lastSeek > 2000) {
                val drift = player.currentPosition / 1000f - est
                if (abs(drift) > 0.8f) {
                    player.seekTo((est * 1000).toLong())
                    lastSeek = now
                }
            }
            delay(400)
        }
    }
    BoxWithConstraints(modifier.clipToBounds().background(Color.Black)) {
        val density = LocalDensity.current
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()
        val sc = if (vsize.width > 0 && vsize.height > 0) max(cw / vsize.width, ch / vsize.height) else 1f
        val w = if (vsize.width > 0) vsize.width * sc else cw
        val h = if (vsize.height > 0) vsize.height * sc else ch
        Cover(song, Modifier.fillMaxSize(), 0.dp, 96.sp, quality = 800)
        if (!failed) {
            AndroidView(
                factory = { c -> TextureView(c).also { player.setVideoTextureView(it) } },
                modifier = Modifier
                    .align(Alignment.Center)
                    .requiredSize(with(density) { w.toDp() }, with(density) { h.toDp() })
                    .graphicsLayer { alpha = if (ready) 1f else 0f }
            )
        }
    }
}
