package com.prismwave.app

import android.content.Context
import android.os.SystemClock
import android.view.TextureView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
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

/** Experiment: muted video-only players kept in sync with the audio player. */
object VideoSync {
    var wantVideo by mutableStateOf(false)
    val has = mutableStateMapOf<String, Boolean>()
    private val urls = HashMap<String, String>()
    private val lows = HashMap<String, String>()

    // the sharp player and the blurred backdrop player, linked together
    var main: ExoPlayer? = null
    var back: ExoPlayer? = null

    /** both videos start only when BOTH are ready, and pause together when either one buffers */
    fun gateOk(): Boolean {
        val ps = listOfNotNull(main, back)
        return ps.isNotEmpty() && ps.all { it.playbackState == Player.STATE_READY }
    }

    val qualityNames = listOf("Best", "1080p", "720p", "480p", "360p")
    private val heights = listOf(4320, 1080, 720, 480, 360)
    var quality by mutableStateOf(0)
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        quality = ctx.getSharedPreferences("prismwave", Context.MODE_PRIVATE).getInt("videoQuality", 0)
    }

    fun setQuality(ctx: Context, q: Int) {
        quality = q
        ctx.getSharedPreferences("prismwave", Context.MODE_PRIVATE).edit().putInt("videoQuality", q).apply()
        has.clear()
        urls.clear()
        lows.clear()
    }

    suspend fun check(song: Song) {
        if (has.containsKey(song.id) || Opts.offline) return
        try {
            val p = Engine.videoPair(song.url, heights[quality.coerceIn(0, heights.lastIndex)])
            if (p != null) {
                urls[song.id] = p.first
                lows[song.id] = p.second
                has[song.id] = true
            } else {
                has[song.id] = false
            }
        } catch (e: Exception) {
        }
    }

    fun url(song: Song, low: Boolean = false): String? = if (low) lows[song.id] else urls[song.id]
    fun showVideo(song: Song): Boolean = wantVideo && has[song.id] == true
}

@Composable
fun ModePills() {
    val itemW = 96.dp
    val x by animateDpAsState(
        if (VideoSync.wantVideo) itemW else 0.dp,
        spring(dampingRatio = 0.7f, stiffness = 380f),
        label = "pillx"
    )
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f)).padding(4.dp)
    ) {
        Box(
            Modifier
                .offset(x = x)
                .width(itemW)
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
        )
        Row {
            listOf("Song" to false, "Video" to true).forEach { (label, v) ->
                val sel = VideoSync.wantVideo == v
                val tc by animateColorAsState(if (sel) Color.Black else Color.White, tween(200), label = "pillt")
                Box(
                    Modifier
                        .width(itemW)
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .softClick { VideoSync.wantVideo = v },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = tc, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** low = true plays a tiny copy of the video, used as the blurred backdrop. */
@Composable
fun VideoStage(s: AppState, song: Song, modifier: Modifier = Modifier, low: Boolean = false) {
    val ctx = LocalContext.current
    val player = remember { ExoPlayer.Builder(ctx).build().apply { volume = 0f } }
    var vsize by remember { mutableStateOf(IntSize.Zero) }
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        if (low) VideoSync.back = player else VideoSync.main = player
        val l = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                vsize = IntSize(videoSize.width, videoSize.height)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) ready = true
                // react at once: pause both the moment one of them buffers
                if (state == Player.STATE_BUFFERING) {
                    VideoSync.main?.playWhenReady = false
                    VideoSync.back?.playWhenReady = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        }
        player.addListener(l)
        onDispose {
            player.removeListener(l)
            if (VideoSync.back === player) VideoSync.back = null
            if (VideoSync.main === player) VideoSync.main = null
            player.release()
        }
    }
    LaunchedEffect(song.id) {
        ready = false
        failed = false
        val url = VideoSync.url(song, low)
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
            // both videos start and pause together
            player.playWhenReady = s.playing && !s.loading && VideoSync.gateOk()
            if (player.playbackState != Player.STATE_IDLE && now - lastSeek > 1500) {
                // the blurred backdrop follows the sharp video, the sharp video follows the audio
                val master = if (low) VideoSync.main else null
                val linked = master != null && master.playbackState != Player.STATE_IDLE
                val target = if (linked) master!!.currentPosition / 1000f else est
                val limit = if (linked) 0.2f else 0.8f
                val drift = player.currentPosition / 1000f - target
                if (abs(drift) > limit) {
                    player.seekTo((target * 1000).toLong())
                    lastSeek = now
                }
            }
            delay(150)
        }
    }
    BoxWithConstraints(modifier.clipToBounds().background(Color.Black)) {
        val density = LocalDensity.current
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()
        val sc = if (vsize.width > 0 && vsize.height > 0) max(cw / vsize.width, ch / vsize.height) else 1f
        val w = if (vsize.width > 0) vsize.width * sc else cw
        val h = if (vsize.height > 0) vsize.height * sc else ch
        Cover(song, Modifier.fillMaxSize(), 0.dp, 96.sp, quality = if (low) 300 else 800)
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
