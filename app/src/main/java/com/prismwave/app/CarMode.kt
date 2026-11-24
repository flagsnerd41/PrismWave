package com.prismwave.app

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private tailrec fun Context.carActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.carActivity()
    else -> null
}

/**
 * Car mode: the screen turns sideways. Player and big controls on the left, lyrics on the right,
 * a slowly moving background tinted by the cover, and lyrics that glide and scale from line to line.
 */
@Composable
fun CarScreen(s: AppState) {
    BackHandler { Pages.close() }
    val ctx = LocalContext.current
    val view = LocalView.current
    DisposableEffect(Unit) {
        val act = ctx.carActivity()
        act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    val song = s.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Night)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        if (song == null) {
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Play a song first", color = TextDim, fontSize = 22.sp)
                Spacer(Modifier.size(16.dp))
                RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { Pages.close() })
            }
            return@Box
        }
        val art by animateColorAsState(rememberArtColor(song), tween(800), label = "carArt")
        val still = Opts.lite || s.batterySaver || s.reduceMotion
        CarBackground(song, art, still)
        Row(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            CarLeft(s, song, art, Modifier.weight(0.46f).fillMaxHeight())
            Spacer(Modifier.width(28.dp))
            CarLyrics(s, song, art, Modifier.weight(0.54f).fillMaxHeight())
        }
    }
}

@Composable
private fun CarBackground(song: Song, art: Color, still: Boolean) {
    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = song, animationSpec = tween(900), label = "carBg") { sg ->
            Cover(
                sg,
                if (Opts.lite) Modifier.fillMaxSize() else Modifier.fillMaxSize().blur(70.dp),
                0.dp, 120.sp, quality = if (Opts.lite) 200 else 400
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
        if (still) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(art.copy(alpha = 0.35f), Color.Transparent))))
        } else {
            // two big soft color blobs drift slowly across the screen
            val tr = rememberInfiniteTransition(label = "carBlobs")
            val t by tr.animateFloat(
                0f, (2.0 * Math.PI).toFloat(),
                infiniteRepeatable(tween(28000, easing = LinearEasing)),
                label = "carT"
            )
            val warm = lerp(art, Color(0xFFFF8A3D), 0.4f)
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val r = h * 0.95f
                val c1 = Offset(w * (0.28f + 0.22f * sin(t)), h * (0.35f + 0.28f * cos(t * 0.8f)))
                val c2 = Offset(w * (0.72f + 0.2f * cos(t * 0.9f)), h * (0.68f + 0.26f * sin(t * 1.1f)))
                drawCircle(Brush.radialGradient(listOf(art.copy(alpha = 0.6f), Color.Transparent), center = c1, radius = r), r, c1)
                drawCircle(Brush.radialGradient(listOf(warm.copy(alpha = 0.45f), Color.Transparent), center = c2, radius = r), r, c2)
            }
        }
    }
}

@Composable
private fun CarRoundButton(icon: ImageVector, size: Dp, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(size).bounceClick(onClick).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
private fun CarLeft(s: AppState, song: Song, art: Color, modifier: Modifier) {
    val total = song.seconds.coerceAtLeast(1).toFloat()
    val pos = s.position.coerceIn(0f, total)
    val apple = IconStyle.apple
    val tint = if (apple) appleTint(art, 0.22f) else Color.White
    Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { Pages.close() })
            Spacer(Modifier.width(12.dp))
            Text("Car mode", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Crossfade(targetState = song, animationSpec = tween(500), label = "carCover") { sg ->
                Cover(sg, Modifier.size(120.dp), 22.dp, 48.sp, quality = 600)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column {
            StyledSeekBar(s, total, lerp(art, Color.White, 0.55f))
            if (SeekPrefs.style == 5) {
                AppleTimeLabels(s, total, art)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmt(pos.toInt()), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    Text("-" + fmt((total - pos).toInt()), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            if (apple) {
                Box(Modifier.size(76.dp).bounceClick { s.prev() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.FastRewind, contentDescription = null, tint = tint, modifier = Modifier.size(46.dp))
                }
                Box(Modifier.size(96.dp).bounceClick { s.toggle() }, contentAlignment = Alignment.Center) {
                    Icon(if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = tint, modifier = Modifier.size(72.dp))
                }
                Box(Modifier.size(76.dp).bounceClick { s.next() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.FastForward, contentDescription = null, tint = tint, modifier = Modifier.size(46.dp))
                }
            } else {
                CarRoundButton(Icons.Rounded.SkipPrevious, 76.dp, Color.White.copy(alpha = 0.14f), Color.White) { s.prev() }
                CarRoundButton(if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 100.dp, s.accent, Color.Black) { s.toggle() }
                CarRoundButton(Icons.Rounded.SkipNext, 76.dp, Color.White.copy(alpha = 0.14f), Color.White) { s.next() }
            }
        }
    }
}

@Composable
private fun CarLyrics(s: AppState, song: Song, art: Color, modifier: Modifier) {
    val ctx = LocalContext.current
    Lyrics.init(ctx)
    LyricsPrefs.init(ctx)
    var lines by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var loaded by remember(song.id) { mutableStateOf(false) }
    LaunchedEffect(song.id) {
        lines = try {
            Lyrics.fetch(song)
        } catch (e: Exception) {
            emptyList()
        }
        loaded = true
    }
    val synced = lines.isNotEmpty() && lines[0].ms >= 0
    val off = remember(song.id) { LyricsPrefs.offset(song.id) }

    // a clock that moves every frame so the lines change exactly on time
    var smoothMs by remember(song.id) { mutableStateOf((s.position * 1000).toLong()) }
    LaunchedEffect(song.id, synced) {
        if (!synced) return@LaunchedEffect
        var lastPos = s.position
        var lastAt = SystemClock.elapsedRealtime()
        while (true) {
            withFrameNanos { }
            val now = SystemClock.elapsedRealtime()
            if (s.position != lastPos) {
                lastPos = s.position
                lastAt = now
            }
            smoothMs = (lastPos * 1000f).toLong() + (if (s.playing) ((now - lastAt) * s.tempo).toLong().coerceAtMost((1300L * s.tempo).toLong()) else 0L)
        }
    }
    val active by remember(lines, synced, off) {
        derivedStateOf { if (synced) lines.indexOfLast { it.ms <= smoothMs + off }.coerceAtLeast(0) else -1 }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(active, lines) {
        if (active >= 0 && lines.isNotEmpty()) {
            val vh = listState.layoutInfo.viewportSize.height
            // the lit line rests about a third from the top, the list glides there
            listState.animateScrollToItem(active, -(vh * 0.3f).toInt())
        }
    }
    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 0.12f to Color.Black, 0.88f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        if (lines.isEmpty()) {
            Text(
                if (loaded) "No lyrics for this song" else "Looking for lyrics...",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 40.dp, bottom = 260.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(lines) { i, line ->
                    if (line.text.isNotBlank()) CarLine(line.text, if (active < 0) 0 else i - active, synced, art)
                }
            }
        }
    }
}

@Composable
private fun CarLine(text: String, rel: Int, synced: Boolean, art: Color) {
    val d = abs(rel)
    val lit = synced && rel == 0
    val targetAlpha = if (!synced) 0.85f else when (d) {
        0 -> 1f
        1 -> 0.55f
        2 -> 0.38f
        else -> 0.24f
    }
    val alpha by animateFloatAsState(targetAlpha, tween(450, easing = FastOutSlowInEasing), label = "carLineA")
    val scale by animateFloatAsState(if (lit || !synced) 1f else 0.9f, spring(dampingRatio = 0.72f, stiffness = 220f), label = "carLineS")
    // the lit line leans in a little, the others rest further back
    val slide by animateFloatAsState(if (lit) 0f else 14f, spring(dampingRatio = 0.8f, stiffness = 180f), label = "carLineX")
    val blurDp by animateFloatAsState(if (synced && !Opts.lite) (d.coerceAtMost(4) * 1.6f) else 0f, tween(450), label = "carLineB")
    val color by animateColorAsState(if (lit) Color.White else lerp(Color.White, art, 0.25f), tween(450), label = "carLineC")
    Text(
        text,
        color = color,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                translationX = slide.dp.toPx()
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .blur(blurDp.dp)
    )
}
