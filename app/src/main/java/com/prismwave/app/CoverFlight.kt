package com.prismwave.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// where the cover of the mini player is on screen
object MiniCover {
    var rect: Rect? = null
}

// the title of the mini player and of the full player, so the title can fly between them
object MiniTitle {
    var rect: Rect? = null
}

object PlayerTitle {
    var rect: Rect? = null
    var sizeSp: Float = 22f
}

object FlightState {
    var running by mutableStateOf(false)
}

// the cover that was tapped on a list or card, so the player can grow out of it
object TapCover {
    var rect: Rect? = null
    var radius: Float = 20f
}

class CoverRef {
    var c: LayoutCoordinates? = null
}

fun Modifier.trackCover(ref: CoverRef): Modifier = this.onGloballyPositioned { ref.c = it }

// play a song and open the full player, growing it out of the cover that was tapped
fun openFrom(s: AppState, song: Song, from: List<Song>, ref: CoverRef, radius: Float) {
    val c = ref.c
    if (c != null && c.isAttached) {
        TapCover.rect = c.boundsInRoot()
        TapCover.radius = radius
    } else {
        TapCover.rect = null
    }
    s.play(song, from)
    s.showPlayer = true
}

private fun Modifier.flightBox(rect: () -> FloatArray): Modifier = this.layout { measurable, constraints ->
    val r = rect()
    val w = r[2].roundToInt().coerceAtLeast(1)
    val h = r[3].roundToInt().coerceAtLeast(1)
    val p = measurable.measure(Constraints.fixed(w, h))
    layout(constraints.maxWidth, constraints.maxHeight) { p.place(r[0].roundToInt(), r[1].roundToInt()) }
}

// Opening the full player: the small cover of the mini player grows and flies to the big cover.
// Closing it: the big cover shrinks and flies back into the mini player.
@Composable
fun CoverFlight(s: AppState) {
    val density = LocalDensity.current
    val cfg = LocalConfiguration.current
    val p = remember { Animatable(0f) }
    val wasOpen = remember { BooleanArray(1) }
    var active by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf<Rect?>(null) }
    var to by remember { mutableStateOf<Rect?>(null) }
    var wide by remember { mutableStateOf(false) }
    var rad0 by remember { mutableStateOf(12f) }
    var rad1 by remember { mutableStateOf(20f) }
    var titleOn by remember { mutableStateOf(false) }
    var tFrom by remember { mutableStateOf<Rect?>(null) }
    var tTo by remember { mutableStateOf<Rect?>(null) }
    var tSize1 by remember { mutableStateOf(22f) }
    LaunchedEffect(s.showPlayer) {
        FlightState.running = false
        val mini = MiniCover.rect
        val wPx = density.density * cfg.screenWidthDp
        val hPx = density.density * cfg.screenHeightDp
        if (s.showPlayer) {
            active = false
            wasOpen[0] = true
            val tap = TapCover.rect
            TapCover.rect = null
            val start = tap ?: mini
            if (start == null || s.reduceMotion) return@LaunchedEffect
            // the Immersive player has a full-width cover stage, the others report where their cover is
            if (s.playerStyle != 1) {
                var tries = 0
                while (SharedCover.rect == null && tries < 8) {
                    delay(16)
                    tries++
                }
            }
            var t2 = 0
            while (PlayerTitle.rect == null && t2 < 10) {
                delay(16)
                t2++
            }
            val sr = if (s.playerStyle == 1) null else SharedCover.rect
            val mt = MiniTitle.rect
            val pt = PlayerTitle.rect
            titleOn = tap == null && mt != null && pt != null
            tFrom = mt
            tTo = pt
            tSize1 = PlayerTitle.sizeSp
            from = start
            to = sr ?: Rect(0f, 0f, wPx, hPx * 0.64f)
            wide = sr == null
            closing = false
            rad0 = if (tap != null) TapCover.radius else 12f
            rad1 = if (sr != null && SharedCover.circle) sr.width / density.density / 2f else if (sr == null) 0f else 20f
            p.snapTo(0f)
            active = true
            FlightState.running = titleOn
            p.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
            FlightState.running = false
            active = false
        } else {
            val was = wasOpen[0]
            wasOpen[0] = false
            active = false
            titleOn = false
            PlayerTitle.rect = null
            // only when the player itself is showing, not the lyrics
            if (!was || mini == null || s.reduceMotion || PlayerStage.stage != 0 || Pages.open == "lyrics") return@LaunchedEffect
            val sr = if (s.playerStyle == 1) null else SharedCover.rect
            from = sr ?: Rect(0f, 0f, wPx, hPx * 0.64f)
            to = mini
            wide = sr == null
            closing = true
            rad0 = if (sr != null && SharedCover.circle) sr.width / density.density / 2f else if (sr == null) 0f else 20f
            rad1 = 12f
            p.snapTo(0f)
            active = true
            p.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            active = false
        }
    }
    val f = from
    val t = to
    if (active && f != null && t != null) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .flightBox {
                        val v = p.value
                        floatArrayOf(
                            f.left + (t.left - f.left) * v,
                            f.top + (t.top - f.top) * v,
                            f.width + (t.width - f.width) * v,
                            f.height + (t.height - f.height) * v
                        )
                    }
                    .graphicsLayer {
                        val v = p.value
                        shape = RoundedCornerShape((rad0 + (rad1 - rad0) * v).coerceAtLeast(0f).dp)
                        clip = true
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        if (wide) {
                            val v = if (closing) 1f - p.value else p.value
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Black,
                                    (1f - 0.45f * v) to Color.Black,
                                    1f to Color.Black.copy(alpha = 1f - v)
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
            ) {
                val song = s.current
                if (song != null) Cover(song, Modifier.fillMaxSize(), 0.dp, 20.sp, quality = 800)
            }
            val ta = tFrom
            val tb = tTo
            if (titleOn && !closing && ta != null && tb != null) {
                val v = p.value
                Text(
                    s.current?.title ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (17f + (tSize1 - 17f) * v).sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset {
                        IntOffset(
                            (ta.left + (tb.left - ta.left) * p.value).roundToInt(),
                            (ta.top + (tb.top - ta.top) * p.value).roundToInt()
                        )
                    }
                )
            }
        }
    }
}
