package com.prismwave.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
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
    LaunchedEffect(s.showPlayer) {
        val mini = MiniCover.rect
        val wPx = density.density * cfg.screenWidthDp
        val hPx = density.density * cfg.screenHeightDp
        if (s.showPlayer) {
            active = false
            wasOpen[0] = true
            if (mini == null || s.reduceMotion) return@LaunchedEffect
            // the Immersive player has a full-width cover stage, the others report where their cover is
            if (s.playerStyle != 1) {
                var tries = 0
                while (SharedCover.rect == null && tries < 8) {
                    delay(16)
                    tries++
                }
            }
            val sr = if (s.playerStyle == 1) null else SharedCover.rect
            from = mini
            to = sr ?: Rect(0f, 0f, wPx, hPx * 0.64f)
            wide = sr == null
            closing = false
            rad0 = 12f
            rad1 = if (sr != null && SharedCover.circle) sr.width / density.density / 2f else if (sr == null) 0f else 20f
            p.snapTo(0f)
            active = true
            p.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
            active = false
        } else {
            val was = wasOpen[0]
            wasOpen[0] = false
            active = false
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
        }
    }
}
