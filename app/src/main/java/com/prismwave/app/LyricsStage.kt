package com.prismwave.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

// The steps of the now playing screen. They all happen on the same screen, nothing opens as a new page:
// 0 = the player, 1 = the half view (cover on top, lyrics underneath), 2 = the full lyrics.
object PlayerStage {
    var stage by mutableStateOf(0)
    var open = false

    var half: Boolean
        get() = stage >= 1
        set(v) {
            stage = if (v) maxOf(stage, 1) else 0
        }

    fun back() {
        stage = (stage - 1).coerceAtLeast(0)
    }
}

// puts a box at an exact spot with an exact size. The spot and size are read while laying out,
// so an animation can move it every frame without rebuilding the screen
private fun Modifier.floatingBox(rect: () -> FloatArray): Modifier = this.layout { measurable, constraints ->
    val r = rect()
    val w = r[2].roundToInt().coerceAtLeast(1)
    val h = r[3].roundToInt().coerceAtLeast(1)
    val p = measurable.measure(Constraints.fixed(w, h))
    layout(constraints.maxWidth, constraints.maxHeight) { p.place(r[0].roundToInt(), r[1].roundToInt()) }
}

/**
 * One cover that moves: it flies up from the player to the top half of the screen, then shrinks into the
 * top left corner while the lyrics fill the screen. st() goes from 0 (player) over 1 (half view) to 2 (full lyrics)
 * and follows your finger while you drag.
 */
@Composable
fun LyricsStage(s: AppState, song: Song, st: () -> Float) {
    val ctx = LocalContext.current
    Lyrics.init(ctx)
    LyricsPrefs.init(ctx)
    val density = LocalDensity.current
    val artRaw = rememberArtColor(song)
    val art by animateColorAsState(artRaw, tween(700), label = "stageArt")
    var lines by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var loaded by remember(song.id) { mutableStateOf(false) }
    LaunchedEffect(song.id) {
        lines = Lyrics.fetch(song)
        loaded = true
    }
    val synced = lines.isNotEmpty() && lines[0].ms >= 0
    var off by remember(song.id) { mutableStateOf(LyricsPrefs.offset(song.id)) }
    var tune by remember { mutableStateOf(false) }
    val startRect = remember { SharedCover.rect }
    val startCircle = remember { SharedCover.circle }
    val fullVisible by remember { derivedStateOf { st() > 1.02f } }
    val halfLive by remember { derivedStateOf { st() < 1.5f } }

    // a clock that moves every frame, only while the full lyrics are showing
    var smoothMs by remember { mutableStateOf((s.position * 1000).toLong()) }
    LaunchedEffect(song.id, fullVisible) {
        if (!fullVisible) return@LaunchedEffect
        var lastPos = s.position
        var lastAt = SystemClock.elapsedRealtime()
        while (true) {
            withFrameNanos { }
            val now = SystemClock.elapsedRealtime()
            if (s.position != lastPos) {
                lastPos = s.position
                lastAt = now
            }
            smoothMs = (lastPos * 1000f).toLong() + (if (s.playing) now - lastAt else 0L)
        }
    }
    val playing by remember(lines, synced, off) {
        derivedStateOf { if (synced) lines.indexOfLast { it.ms <= smoothMs + off } else -1 }
    }
    // a line is lit only while it is being sung. After that the list moves on to the next line,
    // which waits there dim until the song reaches it, like ArchiveTune
    val ended by remember(lines, synced, off) {
        derivedStateOf {
            val pi = playing
            synced && pi >= 0 && smoothMs + off >= lines[pi].ms + sungMs(lines, pi)
        }
    }
    val active by remember(lines, synced, off) {
        derivedStateOf {
            if (!synced) -1 else if (playing < 0) 0 else if (ended && playing + 1 < lines.size && lines[playing + 1].ms - lines[playing].ms > 2500L) playing + 1 else playing
        }
    }
    val litIdx by remember(lines, synced, off) {
        derivedStateOf {
            if (synced && playing >= 0 && !ended) playing else -1
        }
    }
    val posMs = (s.position * 1000).toLong()
    val idxHalf = if (synced) lines.indexOfLast { it.ms <= posMs + off } else -1
    val base = lerp(art, Color.Black, 0.55f)
    val gradTop = remember(art) { tone(art, -14f, 0.70f, 0.36f) }
    val gradBottom = remember(art) { tone(art, 0f, 0.90f, 0.52f) }
    val total = song.seconds.coerceAtLeast(1).toFloat()
    val listState = rememberLazyListState()
    var placed by remember(song.id) { mutableStateOf(false) }
    LaunchedEffect(active, lines, fullVisible) {
        if (fullVisible && active >= 0) {
            withFrameNanos { }
            val target = (listState.layoutInfo.viewportSize.height * 0.35f).toInt()
            if (!placed || abs(active + 1 - listState.firstVisibleItemIndex) > 15) {
                listState.scrollToItem(active + 1, 0)
                listState.scrollBy(-target.toFloat())
                placed = true
            } else {
                val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == active + 1 }
                if (info != null) {
                    listState.animateScrollBy((info.offset - target).toFloat(), tween(850, easing = FastOutSlowInEasing))
                } else {
                    listState.animateScrollToItem(active + 1, -target)
                }
            }
        }
    }
    val blurOn = !Opts.lite && !s.batterySaver
    val fontSize = when (LyricsPrefs.size) {
        0 -> 25.sp
        2 -> 36.sp
        else -> 30.sp
    }
    val lineHeight = when (LyricsPrefs.size) {
        0 -> 32.sp
        2 -> 44.sp
        else -> 38.sp
    }
    val center = LyricsPrefs.center

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = (st() * 1.6f).coerceIn(0f, 1f) }
            .background(base)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
    ) {
        val dp = density.density
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val halfH = hPx * 0.56f
        val smallSize = 46f * dp
        val smallLeft = 16f * dp
        val smallTop = WindowInsets.statusBars.getTop(density).toFloat() + 14f * dp
        val rowH = 56f * dp

        fun ease(x: Float): Float = FastOutSlowInEasing.transform(x.coerceIn(0f, 1f))

        // left, top, width, height of the cover at this step
        fun coverBox(v: Float): FloatArray {
            val s1 = ease(v)
            val m = ease(v - 1f)
            val a = startRect
            val l0 = a?.left ?: 0f
            val t0 = a?.top ?: 0f
            val w0 = a?.width ?: wPx
            val h0 = a?.height ?: halfH
            var l = l0 + (0f - l0) * s1
            var t = t0 + (0f - t0) * s1
            var w = w0 + (wPx - w0) * s1
            var h = h0 + (halfH - h0) * s1
            l += (smallLeft - l) * m
            t += (smallTop - t) * m
            w += (smallSize - w) * m
            h += (smallSize - h) * m
            return floatArrayOf(l, t, w, h)
        }

        // 1. the soft layers behind everything
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - (st() - 1f).coerceIn(0f, 1f) }) {
            SwirlBackground(art, s.reduceMotion || Opts.lite)
        }
        // the full lyrics get a smooth gradient of the cover colors, like ArchiveTune
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (st() - 1f).coerceIn(0f, 1f) }
                .background(Brush.verticalGradient(0f to gradTop, 0.5f to lerp(gradTop, gradBottom, 0.45f), 1f to gradBottom))
        )
        if (!Opts.lite) {
            Cover(
                song,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.85f * (1f - (st() - 1f).coerceIn(0f, 1f)) }
                    .blur(70.dp),
                0.dp, 90.sp, quality = 300
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (1f - (st() - 1f).coerceIn(0f, 1f) * 1.2f).coerceIn(0f, 1f) }
                .background(Brush.verticalGradient(0.40f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)))
        )

        // 2. the cover: it flies up, then shrinks into the corner
        Box(
            Modifier
                .floatingBox { coverBox(st()) }
                .graphicsLayer {
                    val v = st()
                    val m = (v - 1f).coerceIn(0f, 1f)
                    val r0 = if (startCircle && startRect != null) startRect.width / this.density / 2f else 20f
                    val rad = if (m > 0f) 10f * m else r0 * (1f - v.coerceIn(0f, 1f))
                    shape = RoundedCornerShape(rad.dp)
                    clip = true
                    alpha = (v * 6f).coerceIn(0f, 1f)
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    // the bottom of the half cover melts into the blur below it
                    val v = st()
                    val k = ease(v) * (1f - ease(v - 1f))
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black,
                            (1f - 0.45f * k) to Color.Black,
                            1f to Color.Black.copy(alpha = 1f - k)
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                .then(
                    if (fullVisible) {
                        Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            PlayerStage.stage = 1
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            Cover(song, Modifier.fillMaxSize(), 0.dp, 90.sp, quality = 1200)
        }

        // 3. the half view lyrics: the line that plays is clear, the next ones fade and blur away
        Box(
            Modifier
                .floatingBox { floatArrayOf(24f * dp, halfH + 6f * dp, wPx - 48f * dp, (hPx - halfH - 130f * dp).coerceAtLeast(1f)) }
                .graphicsLayer {
                    val v = st()
                    val m = (v - 1f).coerceIn(0f, 1f)
                    alpha = (1f - m * 2.2f).coerceIn(0f, 1f) * ((v - 0.5f) * 2f).coerceIn(0f, 1f)
                    translationY = -m * 70f * this.density
                }
                .then(
                    if (halfLive) {
                        Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            PlayerStage.stage = 2
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            if (lines.isEmpty()) {
                Text(
                    if (loaded) "No lyrics found for this song" else "Loading lyrics...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (k in 0 until 4) {
                        val ln = lines.getOrNull(if (idxHalf >= 0) idxHalf + k else k)
                        if (ln != null) {
                            HalfLine(ln.text.ifEmpty { "..." }, k, idxHalf, s.reduceMotion)
                        }
                    }
                }
            }
        }

        // 4. title and pause button: at the bottom of the half cover, then up next to the small cover
        Row(
            Modifier
                .floatingBox {
                    val m = ease(st() - 1f)
                    val x = (24f + (74f - 24f) * m) * dp
                    val y = (halfH - 84f * dp) + ((smallTop + smallSize / 2f - rowH / 2f) - (halfH - 84f * dp)) * m
                    floatArrayOf(x, y, wPx - x - 16f * dp, rowH)
                }
                .graphicsLayer { alpha = ((st() - 0.55f) / 0.35f).coerceIn(0f, 1f) }
                .pointerInput(Unit) {
                    var acc = 0f
                    detectVerticalDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = { if (acc > 70f && PlayerStage.stage == 2) PlayerStage.stage = 1 },
                        onVerticalDrag = { _, dy -> acc += dy }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (fullVisible) {
                RoundBtn(Icons.Rounded.Tune, 0.9f, { tune = !tune }, 40.dp)
                Spacer(Modifier.width(8.dp))
            }
            RoundBtn(if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 0.9f, { s.toggle() }, 40.dp)
            if (fullVisible) {
                Spacer(Modifier.width(8.dp))
                RoundBtn(Icons.Rounded.KeyboardArrowDown, 0.9f, { PlayerStage.stage = 1 }, 40.dp)
            }
        }

        // 5. the full lyrics
        if (fullVisible) {
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val m = (st() - 1f).coerceIn(0f, 1f)
                        alpha = ((m - 0.25f) / 0.6f).coerceIn(0f, 1f)
                        translationY = (1f - m) * 0.2f * size.height
                    }
                    .navigationBarsPadding()
            ) {
                Spacer(Modifier.height(with(density) { (smallTop + smallSize + 8f * dp).toDp() }))
                if (tune) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TuneBtn("-0.2s") {
                            off -= 200L
                            LyricsPrefs.setOffset(song.id, off)
                        }
                        Text(
                            if (off == 0L) "In sync" else (if (off > 0) "+" else "") + (off / 100) / 10f + "s",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        TuneBtn("+0.2s") {
                            off += 200L
                            LyricsPrefs.setOffset(song.id, off)
                        }
                        TuneBtn("A-") {
                            LyricsPrefs.size = (LyricsPrefs.size - 1).coerceAtLeast(0)
                            LyricsPrefs.save()
                        }
                        TuneBtn("A+") {
                            LyricsPrefs.size = (LyricsPrefs.size + 1).coerceAtMost(2)
                            LyricsPrefs.save()
                        }
                        TuneBtn(if (center) "Left" else "Center") {
                            LyricsPrefs.center = !LyricsPrefs.center
                            LyricsPrefs.save()
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (lines.isEmpty()) {
                        Text(
                            if (loaded) "No lyrics found for this song" else "Loading lyrics...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 40.dp)
                        )
                    } else {
                        LazyColumn(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer(alpha = 0.99f)
                                .drawWithContent {
                                    drawContent()
                                    val px = 80.dp.toPx()
                                    drawRect(
                                        brush = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = px),
                                        blendMode = BlendMode.DstIn
                                    )
                                    drawRect(
                                        brush = Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, startY = size.height - px, endY = size.height),
                                        blendMode = BlendMode.DstIn
                                    )
                                },
                            state = listState,
                            contentPadding = PaddingValues(top = 0.dp, bottom = 240.dp)
                        ) {
                            item { Spacer(Modifier.height(120.dp)) }
                            itemsIndexed(lines) { i, ln ->
                                val dist = if (synced && active >= 0) abs(i - active) else 0
                                val on = !synced || i == litIdx
                                val a by animateFloatAsState(
                                    if (on) 1f
                                    else if (dist == 0) 0.50f
                                    else if (dist == 1) 0.52f
                                    else if (dist == 2) 0.30f
                                    else if (dist == 3) 0.18f
                                    else 0.14f,
                                    tween(if (on) 330 else 500, easing = FastOutSlowInEasing),
                                    label = "sa"
                                )
                                val sc by animateFloatAsState(
                                    if (!synced || dist == 0) 1f else 0.95f,
                                    spring(dampingRatio = 0.72f, stiffness = 260f),
                                    label = "ss"
                                )
                                val br by animateDpAsState(
                                    if (dist == 0 || !blurOn || !synced) 0.dp
                                    else (if (dist == 1) 1.5f else if (dist == 2) 3.5f else 6f).dp,
                                    tween(300, easing = FastOutSlowInEasing),
                                    label = "sb"
                                )
                                val drift = remember { Animatable(0f) }
                                var prevActive by remember { mutableStateOf(active) }
                                LaunchedEffect(active) {
                                    if (prevActive != active) {
                                        prevActive = active
                                        val d = abs(i - active)
                                        if (synced && active >= 0 && d in 1..6 && !s.reduceMotion) {
                                            val dir = if (i < active) -1f else 1f
                                            drift.snapTo(dir * 16f)
                                            drift.animateTo(0f, tween(900, delayMillis = d * 70, easing = FastOutSlowInEasing))
                                            
                                        }
                                    }
                                }
                                val endMs = if (i + 1 < lines.size) lines[i + 1].ms else ln.ms + 5000L
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            enabled = synced
                                        ) { s.seekTo(((ln.ms - off).coerceAtLeast(0L)) / 1000f) }
                                        .padding(horizontal = 26.dp, vertical = 11.dp)
                                        .graphicsLayer {
                                            alpha = a
                                            translationY = drift.value * this.density
                                            scaleX = sc
                                            scaleY = sc
                                            transformOrigin = TransformOrigin(if (center) 0.5f else 0f, 0.5f)
                                        }
                                        .then(if (br > 0.dp) Modifier.blur(br, BlurredEdgeTreatment.Unbounded) else Modifier),
                                    contentAlignment = if (center) Alignment.Center else Alignment.CenterStart
                                ) {
                                    if (ln.text.isEmpty()) {
                                        PauseDots(ln.ms, endMs, on && synced, { smoothMs + off })
                                    } else {
                                        BounceText(ln.text, on && synced, synced, fontSize, lineHeight, center)
                                    }
                                }
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
}

// a color from the cover with its hue turned, a maximum saturation and a fixed brightness
private fun tone(c: Color, turn: Float, maxSat: Float, bright: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    hsv[0] = (hsv[0] + turn + 360f) % 360f
    hsv[1] = (hsv[1] + 0.15f).coerceAtMost(maxSat)
    hsv[2] = bright
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// how long a line is sung: a guess from its length, never longer than the gap to the next line
private fun sungMs(lines: List<LyricLine>, i: Int): Long {
    val gap = if (i + 1 < lines.size) lines[i + 1].ms - lines[i].ms else 6000L
    return (lines[i].text.length * 95L).coerceIn(1800L, 8000L).coerceAtMost((gap - 200L).coerceAtLeast(500L))
}

// the line lights up when it is sung and its words bounce up one after another (like ArchiveTune)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BounceText(
    text: String,
    lit: Boolean,
    animate: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    center: Boolean
) {
    val words = remember(text) { text.split(" ") }
    val scales = remember(words.size) { List(words.size) { Animatable(1f) } }
    val floats = remember(words.size) { List(words.size) { Animatable(0f) } }
    LaunchedEffect(lit) {
        if (false) {
            words.indices.forEach { i ->
                launch {
                    delay(i * 40L)
                    scales[i].animateTo(1.045f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
                    scales[i].animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    delay(i * 40L)
                    floats[i].animateTo(-5f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
                    floats[i].animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                }
            }
        }
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (center) Arrangement.Center else Arrangement.Start
    ) {
        words.forEachIndexed { i, w ->
            Text(
                if (i < words.lastIndex) w + " " else w,
                color = Color.White,
                fontFamily = PrismFont,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    scaleX = scales[i].value
                    scaleY = scales[i].value
                    translationY = floats[i].value * this.density
                }
            )
        }
    }
}

@Composable
private fun HalfLine(text: String, k: Int, idx: Int, still: Boolean) {
    val drift = remember { Animatable(0f) }
    val appear = remember { Animatable(1f) }
    var prev by remember { mutableStateOf(idx) }
    LaunchedEffect(idx) {
        if (prev != idx) {
            prev = idx
            if (!still) {
                drift.snapTo(16f)
                appear.snapTo(0.3f)
                launch { appear.animateTo(1f, tween(900, delayMillis = k * 70, easing = FastOutSlowInEasing)) }
                drift.animateTo(0f, tween(900, delayMillis = k * 70, easing = FastOutSlowInEasing))
            }
        }
    }
    Text(
        text,
        color = Color.White,
        fontSize = if (k == 0) 27.sp else 25.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .graphicsLayer {
                alpha = (if (k == 0) 1f else (0.58f - 0.16f * k).coerceAtLeast(0.14f)) * appear.value
                translationY = drift.value * this.density
            }
            .then(if (k > 0 && !Opts.lite) Modifier.blur((k * 1.6f).dp) else Modifier)
    )
}
