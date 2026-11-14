package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Always on display. Idle on the full player: after a while it goes to the half view (cover on top,
// lyrics underneath), after another while to a black screen with a small cover, the controls and the
// lyrics at the bottom with the app icon. Any touch on the empty black screen goes back to the player.
object Aod {
    var enabled by mutableStateOf(true)
    var halfMin by mutableStateOf(1)
    var blackMin by mutableStateOf(1)

    // the black screen is showing
    var active by mutableStateOf(false)

    // the half view was opened by the idle timer, not by you
    var auto = false

    var lastTouch = SystemClock.elapsedRealtime()
    private var p: SharedPreferences? = null

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_aod", Context.MODE_PRIVATE)
        p = sp
        enabled = sp.getBoolean("on", true)
        halfMin = sp.getInt("half", 1).coerceIn(1, 5)
        blackMin = sp.getInt("black", 1).coerceIn(1, 5)
    }

    fun save() {
        p?.edit()?.putBoolean("on", enabled)?.putInt("half", halfMin)?.putInt("black", blackMin)?.apply()
    }

    fun reset() {
        active = false
        auto = false
        lastTouch = SystemClock.elapsedRealtime()
    }

    // any touch on the player: the idle clock starts again, and the half view that came by itself closes
    fun touch() {
        lastTouch = SystemClock.elapsedRealtime()
        if (auto && !active) {
            auto = false
            PlayerStage.stage = 0
        }
    }

    // wake up from the black screen
    fun exit() {
        active = false
        auto = false
        lastTouch = SystemClock.elapsedRealtime()
        PlayerStage.stage = 0
    }
}

// watches every touch on the player without taking it away from the buttons
fun Modifier.aodTouch(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            Aod.touch()
        }
    }
}

// lives inside the full player: the idle timer, the screen-on flag and the black screen
@Composable
fun AodHost(s: AppState, song: Song, blocked: Boolean) {
    val ctx = LocalContext.current
    Aod.init(ctx.applicationContext)
    val view = LocalView.current
    val keepOn = Aod.enabled && s.playing
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }
    DisposableEffect(Unit) {
        Aod.reset()
        onDispose { Aod.reset() }
    }
    val blockedNow by rememberUpdatedState(blocked)
    LaunchedEffect(Aod.enabled) {
        if (!Aod.enabled) return@LaunchedEffect
        while (true) {
            delay(1000)
            // a menu or another page is open: you are using the phone, so the clock waits
            if (blockedNow || !Pages.open.isNullOrEmpty()) {
                Aod.lastTouch = SystemClock.elapsedRealtime()
                continue
            }
            val idle = SystemClock.elapsedRealtime() - Aod.lastTouch
            val first = Aod.halfMin * 60_000L
            val second = first + Aod.blackMin * 60_000L
            if (!Aod.active && !Aod.auto && PlayerStage.stage == 0 && idle >= first) {
                Aod.auto = true
                PlayerStage.stage = 1
            } else if (!Aod.active && Aod.auto && PlayerStage.stage == 1 && idle >= second) {
                Aod.active = true
            }
        }
    }
    BackHandler(enabled = Aod.active) { Aod.exit() }
    val show by animateFloatAsState(if (Aod.active) 1f else 0f, tween(1000, easing = FastOutSlowInEasing), label = "aodShow")
    if (show > 0.001f) AodScreen(s, song, show)
}

@Composable
private fun AodScreen(s: AppState, song: Song, show: Float) {
    val ctx = LocalContext.current
    Lyrics.init(ctx)
    var lines by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    LaunchedEffect(song.id) { lines = Lyrics.fetch(song) }
    val synced = lines.isNotEmpty() && lines[0].ms >= 0
    val posMs = (s.position * 1000).toLong()
    val idx = if (synced) lines.indexOfLast { it.ms <= posMs + 250 } else -1
    val line1 = if (idx >= 0) lines[idx].text.ifEmpty { "..." } else song.title
    val line2 = if (idx >= 0) (lines.getOrNull(idx + 1)?.text ?: song.artist) else song.artist
    val total = song.seconds.coerceAtLeast(1).toFloat()

    // burn-in protection: the whole layout moves a few dp every 40 seconds
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(40_000)
            step = (step + 1) % 4
        }
    }
    val dxs = listOf(0.dp, 10.dp, 0.dp, (-10).dp)
    val dys = listOf(0.dp, 8.dp, 16.dp, 8.dp)
    val dx by animateDpAsState(dxs[step], tween(6000), label = "aodDx")
    val dy by animateDpAsState(dys[step], tween(6000), label = "aodDy")

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = show }
            .background(Color.Black)
            .then(
                if (Aod.active) {
                    // the first touch on an empty spot wakes the player up, and the rest of that touch is eaten
                    Modifier.pointerInput(Aod.active) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown()
                                down.consume()
                                Aod.exit()
                                do {
                                    val e = awaitPointerEvent()
                                    e.changes.forEach { it.consume() }
                                } while (e.changes.any { it.pressed })
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .offset(dx, dy)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.55f))
            // the player, shrunk a bit
            Cover(
                song,
                Modifier
                    .fillMaxWidth(0.52f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        val k = 1.28f - 0.28f * show
                        scaleX = k
                        scaleY = k
                        alpha = 0.85f
                    },
                22.dp, 60.sp, quality = 800
            )
            Spacer(Modifier.height(20.dp))
            Text(
                song.title,
                color = Color.White.copy(alpha = 0.92f),
                fontFamily = PrismFont,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                color = Color.White.copy(alpha = 0.55f),
                fontFamily = PrismFont,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(18.dp))
            SeekBar({ s.position }, total, { s.seekTo(it) }, Modifier.fillMaxWidth().graphicsLayer { alpha = 0.7f })
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.bounceClick { s.prev() }.size(64.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(34.dp))
                }
                Box(Modifier.bounceClick { s.toggle() }.size(76.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(50.dp)
                    )
                }
                Box(Modifier.bounceClick { s.next() }.size(64.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            // the lyrics at the bottom, with the app icon, in the app font
            Row(
                Modifier.fillMaxWidth().padding(bottom = 30.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                AppLogo(40.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = line1,
                        transitionSpec = {
                            (slideInVertically(tween(320)) { it / 2 } + fadeIn(tween(320))) togetherWith
                                (slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(160)))
                        },
                        label = "aodLine1"
                    ) { t ->
                        Text(
                            t,
                            color = Color.White.copy(alpha = 0.9f),
                            fontFamily = PrismFont,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AnimatedContent(
                        targetState = line2,
                        transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(160)) },
                        label = "aodLine2"
                    ) { t ->
                        Text(
                            t,
                            color = Color.White.copy(alpha = 0.42f),
                            fontFamily = PrismFont,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
