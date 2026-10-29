package com.prismwave.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Lyrics page in the ArchiveTune style: header (cover, title, close and more buttons),
 * a background tinted by the cover, big bold left-aligned lines, the current line sharp
 * and all other lines blurred and faded by their distance from it.
 */
@Composable
fun LyricsScreen(s: AppState) {
    BackHandler { Pages.close() }
    val song = s.current
    if (song == null) {
        LaunchedEffect(Unit) { Pages.close() }
        return
    }
    val reduce = s.reduceMotion
    val art = rememberArtColor(song)
    val topC by animateColorAsState(lerp(art, Night, 0.80f), tween(700), label = "lyTop")
    val botC by animateColorAsState(lerp(art, Color.Black, 0.30f), tween(700), label = "lyBot")
    var lines by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var busy by remember(song.id) { mutableStateOf(true) }
    LaunchedEffect(song.id) {
        busy = true
        lines = if (!Opts.offline) Lyrics.fetch(song) else emptyList()
        busy = false
    }
    val synced = lines.isNotEmpty() && lines[0].ms >= 0
    val posMs = (s.position * 1000).toLong()
    val active = if (synced) lines.indexOfLast { it.ms <= posMs + 250 } else -1
    val listState = rememberLazyListState()
    // the current line glides up to the same spot every time
    LaunchedEffect(active, lines) {
        if (active >= 0) {
            val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == active }
            if (info != null) {
                listState.animateScrollBy(info.offset.toFloat(), tween(650, easing = FastOutSlowInEasing))
            } else {
                listState.animateScrollToItem(active)
            }
        }
    }
    // cover and title come down into the header
    val hdr = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) hdr.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
    val total = song.seconds.coerceAtLeast(1).toFloat()
    val blurOn = !Opts.lite && !s.batterySaver

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topC, botC)))
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .graphicsLayer {
                        alpha = hdr.value
                        translationY = (1f - hdr.value) * -80f
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cover(song, Modifier.size(46.dp), 10.dp, 16.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontFamily = PrismFont,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                RoundBtn(Icons.Rounded.Close, 0.9f, { Pages.close() }, 42.dp)
                Spacer(Modifier.width(8.dp))
                RoundBtn(Icons.Rounded.MoreHoriz, 0.9f, { Pages.show("details", song) }, 42.dp)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (busy) {
                    LoadingRow(s)
                } else if (lines.isEmpty()) {
                    Text(
                        "No lyrics found for this song",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 18.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 40.dp)
                    )
                } else {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (Opts.lite) Modifier else Modifier
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Black,
                                                0.80f to Color.Black,
                                                1f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                            ),
                        state = listState,
                        contentPadding = PaddingValues(top = 70.dp, bottom = 280.dp)
                    ) {
                        itemsIndexed(lines) { i, ln ->
                            val dist = if (synced && active >= 0) kotlin.math.abs(i - active) else 0
                            val on = !synced || i == active
                            val a by animateFloatAsState(
                                if (on) 1f else (0.5f - 0.07f * dist).coerceAtLeast(0.16f),
                                tween(380),
                                label = "lya"
                            )
                            val sc by animateFloatAsState(
                                if (on && synced) 1f else if (synced) 0.86f else 1f,
                                spring(dampingRatio = 0.72f, stiffness = 260f),
                                label = "lysc"
                            )
                            val br by animateDpAsState(
                                if (on || !blurOn) 0.dp else (1.5f + 1.1f * dist).coerceAtMost(7f).dp,
                                tween(380),
                                label = "lyb"
                            )
                            val enter = remember { Animatable(if (i < 9 && !reduce) 0f else 1f) }
                            LaunchedEffect(Unit) {
                                if (i < 9 && !reduce) {
                                    delay(160L + i * 60L)
                                    enter.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
                                }
                            }
                            Text(
                                if (ln.text.isEmpty()) "♪" else ln.text,
                                color = Color.White,
                                fontFamily = PrismFont,
                                fontSize = 30.sp,
                                lineHeight = 38.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        enabled = synced
                                    ) { s.seekTo(ln.ms / 1000f) }
                                    .padding(horizontal = 26.dp, vertical = 11.dp)
                                    .graphicsLayer {
                                        alpha = a * enter.value
                                        translationY = (1f - enter.value) * 110f
                                        scaleX = sc
                                        scaleY = sc
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                    }
                                    .then(
                                        if (br > 0.dp) Modifier.blur(br, BlurredEdgeTreatment.Unbounded) else Modifier
                                    )
                            )
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                SeekBar({ s.position }, total, { s.seekTo(it) }, Modifier.fillMaxWidth())
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundBtn(Icons.Rounded.SkipPrevious, 0.9f, { s.prev() }, 50.dp)
                    PlayButton(s, 64.dp, 32.dp)
                    RoundBtn(Icons.Rounded.SkipNext, 0.9f, { s.next() }, 50.dp)
                }
            }
        }
    }
}
