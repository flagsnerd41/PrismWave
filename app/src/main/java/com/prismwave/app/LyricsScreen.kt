package com.prismwave.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars

import androidx.compose.foundation.gestures.detectVerticalDragGestures

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lyrics page: the current line fills word by word like karaoke, long pauses show three
 * dots, the background slowly moves with the cover colors, and you can fix the sync,
 * the text size and the alignment with the tune button.
 */
@Composable
fun LyricsScreen(s: AppState) {
    val song = s.current
    if (song == null) {
        LaunchedEffect(Unit) { Pages.close() }
        return
    }
    val ctx = LocalContext.current
    Lyrics.init(ctx)
    LyricsPrefs.init(ctx)
    val reduce = s.reduceMotion
    val art = rememberArtColor(song)
    val topC by animateColorAsState(lerp(art, Night, 0.80f), tween(700), label = "lyTop")
    val botC by animateColorAsState(lerp(art, Color.Black, 0.30f), tween(700), label = "lyBot")
    var lines by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var busy by remember(song.id) { mutableStateOf(true) }
    LaunchedEffect(song.id) {
        busy = true
        lines = Lyrics.fetch(song)
        busy = false
    }
    val synced = lines.isNotEmpty() && lines[0].ms >= 0
    var off by remember(song.id) { mutableStateOf(LyricsPrefs.offset(song.id)) }
    var tune by remember { mutableStateOf(false) }

    // a clock that moves every frame, so the word fill is smooth between the 1 second updates
    var smoothMs by remember { mutableStateOf((s.position * 1000).toLong()) }
    LaunchedEffect(song.id) {
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
    val active by remember(lines, synced, off) {
        derivedStateOf {
            if (synced) lines.indexOfLast { it.ms <= smoothMs + off + 250 } else -1
        }
    }
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
    // the cover flies from the player into the header corner
    val shared = remember { SharedCover.rect }
    val density = LocalDensity.current
    var slot by remember { mutableStateOf<Rect?>(null) }
    val fly = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) fly.animateTo(1f, tween(620, easing = FastOutSlowInEasing)) }
    // the player fades in and out together with the cover flying
    LaunchedEffect(Unit) { snapshotFlow { fly.value }.collect { LyricsMorph.p = it } }
    DisposableEffect(Unit) { onDispose { LyricsMorph.p = 0f } }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    // closing: the cover grows back to the player, the lyrics slide down to the lower half, then the controls return
    val close: () -> Unit = {
        if (!closing) {
            if (shared != null && !reduce) {
                closing = true
                scope.launch {
                    fly.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
                    Pages.close()
                }
            } else {
                Pages.close()
            }
        }
    }
    BackHandler { close() }
    val total = song.seconds.coerceAtLeast(1).toFloat()
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

    val lyTop = with(density) { maxOf(WindowInsets.displayCutout.getTop(this), WindowInsets.statusBars.getTop(this)).toDp() } + 24.dp

    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (shared != null) (fly.value * 2f).coerceIn(0f, 1f) else 1f }
                .background(Brush.verticalGradient(listOf(topC, botC)))
        ) {
            if (!Opts.lite && !s.batterySaver) LyricsBackdrop(art, reduce)
        }
        Column(Modifier.fillMaxSize().padding(top = lyTop).navigationBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var acc = 0f
                        detectVerticalDragGestures(
                            onDragStart = { acc = 0f },
                            onDragEnd = { if (acc > 80f) close() },
                            onVerticalDrag = { _, dy -> acc += dy }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(46.dp).onGloballyPositioned { slot = it.boundsInRoot() }) {
                    val sr = shared
                    val sl = slot
                    if (sr != null && sl != null && sr.width > 1f && sl.width > 1f) {
                        val bigDp = with(density) { sr.width.toDp() }
                        Box(
                            Modifier
                                .requiredSize(bigDp)
                                .graphicsLayer {
                                    val p = fly.value
                                    val k = sl.width / sr.width
                                    val scale = 1f + (k - 1f) * p
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    translationX = (1f - p) * (sr.left - sl.left)
                                    translationY = (1f - p) * (sr.top - sl.top)
                                    scaleX = scale
                                    scaleY = scale
                                    val eff = 20f + (10f - 20f) * p
                                    shape = RoundedCornerShape((eff / scale).dp)
                                    clip = true
                                }
                        ) {
                            Cover(song, Modifier.fillMaxSize(), 0.dp, 60.sp, quality = 600)
                        }
                    } else if (sr == null) {
                        Cover(song, Modifier.fillMaxSize(), 10.dp, 16.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .graphicsLayer {
                            val p = fly.value
                            val sr = shared
                            val sl = slot
                            alpha = ((p - 0.15f) / 0.55f).coerceIn(0f, 1f)
                            translationY = if (sr != null && sl != null) {
                                (1f - p) * (sr.bottom + 12.dp.toPx() - sl.top)
                            } else {
                                (1f - p) * -80f
                            }
                        }
                ) {
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
                Row(
                    Modifier.graphicsLayer { alpha = ((fly.value - 0.35f) / 0.5f).coerceIn(0f, 1f) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundBtn(Icons.Rounded.Tune, 0.9f, { tune = !tune }, 42.dp)
                    Spacer(Modifier.width(8.dp))
                    RoundBtn(if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 0.9f, { s.toggle() }, 42.dp)
                    Spacer(Modifier.width(8.dp))
                    RoundBtn(Icons.Rounded.Close, 0.9f, { close() }, 42.dp)
                }
            }
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
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (closing) {
                            translationY = (1f - fly.value) * size.height * 0.55f
                            alpha = (fly.value * 1.6f).coerceIn(0f, 1f)
                        }
                    }
            ) {
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
                            val dist = if (synced && active >= 0) abs(i - active) else 0
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
                                        alpha = a * enter.value
                                        translationY = (1f - enter.value) * 110f
                                        scaleX = sc
                                        scaleY = sc
                                        transformOrigin = TransformOrigin(if (center) 0.5f else 0f, 0.5f)
                                    }
                                    .then(
                                        if (br > 0.dp) Modifier.blur(br, BlurredEdgeTreatment.Unbounded) else Modifier
                                    ),
                                contentAlignment = if (center) Alignment.Center else Alignment.CenterStart
                            ) {
                                if (ln.text.isEmpty()) {
                                    PauseDots(ln.ms, endMs, on && synced, { smoothMs + off })
                                } else {
                                    LyricText(ln.text, ln.ms, endMs, on && synced, { smoothMs + off }, fontSize, lineHeight, center)
                                }
                            }
                        }
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (closing) {
                            alpha = fly.value
                            translationY = (1f - fly.value) * 90f
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
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

@Composable
fun TuneBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// The line that is playing fills word by word. The time of each word is guessed from its
// length, because the lyric files only know when a whole line starts.
@Composable
fun LyricText(
    text: String,
    startMs: Long,
    endMs: Long,
    on: Boolean,
    clock: () -> Long,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    center: Boolean
) {
    val dim = Color.White.copy(alpha = 0.45f)
    val body = if (!on) {
        buildAnnotatedString { append(text) }
    } else {
        val dur = (endMs - startMs).coerceIn(1200L, 9000L).toFloat()
        val p = (((clock() - startMs) / dur) / 0.92f).coerceIn(0f, 1f)
        val words = text.split(" ")
        val weights = words.map { it.length + 1 }
        val sum = weights.sum().toFloat()
        var cum = 0
        buildAnnotatedString {
            words.forEachIndexed { i, w ->
                val a = cum / sum
                val b = (cum + weights[i]) / sum
                cum += weights[i]
                val f = ((p - a) / (b - a)).coerceIn(0f, 1f)
                withStyle(SpanStyle(color = lerp(dim, Color.White, f))) { append(w) }
                if (i < words.lastIndex) append(" ")
            }
        }
    }
    Text(
        body,
        color = Color.White,
        fontFamily = PrismFont,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = FontWeight.Bold,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}

// three dots that fill up during an instrumental part
@Composable
fun PauseDots(startMs: Long, endMs: Long, on: Boolean, clock: () -> Long) {
    val p = if (on) ((clock() - startMs).toFloat() / (endMs - startMs).coerceAtLeast(1L)).coerceIn(0f, 1f) else 0f
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 3) {
            val f = ((p * 3f) - i).coerceIn(0f, 1f)
            Box(
                Modifier
                    .size((10f + 6f * f).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f + 0.7f * f))
            )
        }
    }
}

// slow, soft blobs of the cover color moving behind the words
@Composable
private fun LyricsBackdrop(art: Color, still: Boolean) {
    val tr = rememberInfiniteTransition(label = "lybg")
    val t by tr.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "lybgt"
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val a = if (still) 0f else t * 2f * PI.toFloat()
        fun blob(cx: Float, cy: Float, r: Float, c: Color, al: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(c.copy(alpha = al), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }
        blob(w * (0.25f + 0.20f * sin(a)), h * (0.25f + 0.12f * cos(a)), w * 0.95f, lerp(art, Color.White, 0.25f), 0.42f)
        blob(w * (0.80f + 0.15f * cos(a + 1f)), h * (0.55f + 0.15f * sin(a + 2f)), w * 1.0f, art, 0.38f)
        blob(w * (0.40f + 0.25f * sin(a + 4f)), h * (0.90f + 0.06f * cos(a)), w * 1.1f, lerp(art, Color(0xFF8AB4F8), 0.35f), 0.30f)
    }
}
