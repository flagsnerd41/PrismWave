package com.prismwave.app

import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.DirectionsCar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.animation.core.animateFloat

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material.icons.rounded.ArrowBack

import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.foundation.horizontalScroll
import android.media.audiofx.AudioEffect
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import android.os.Build
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.ui.unit.TextUnit
import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NavBar(s: AppState) {
    val items = listOf(
        Triple(Screen.Home, Icons.Rounded.Explore, "Home"),
        Triple(Screen.Search, Icons.Rounded.Search, "Search"),
        Triple(Screen.Library, Icons.Rounded.GridView, "Library")
    )
    LiquidGlass(s.glassAlpha(), Modifier.fillMaxWidth().height(78.dp), 30.dp) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (screen, icon, label) ->
                val sel = s.screen == screen
                Column(
                    Modifier
                        .weight(1f)
                        .softClick {
                            s.screen = screen
                            s.libraryOpen = null
                            s.openPlaylist = null
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val pillW by animateDpAsState(
                        if (sel) 68.dp else 34.dp,
                        spring(dampingRatio = 0.55f, stiffness = 380f),
                        label = "pillw"
                    )
                    val pillC by animateColorAsState(
                        if (sel) s.accent.copy(alpha = 0.28f) else Color.Transparent,
                        tween(250),
                        label = "pillc"
                    )
                    val iconC by animateColorAsState(if (sel) s.accent else TextDim, tween(250), label = "iconc")
                    Box(
                        Modifier
                            .width(pillW)
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(pillC),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconC)
                    }
                    Text(label, color = iconC, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(s: AppState) {
    val song = s.current ?: return
    val art = rememberArtColor(song)
    var shown by remember { mutableStateOf(false) }
    val glow = remember { Animatable(1f) }
    val pulseT = androidx.compose.animation.core.rememberInfiniteTransition(label = "mpp")
    val pulseA by pulseT.animateFloat(
        0.10f, 0.28f,
        androidx.compose.animation.core.infiniteRepeatable(tween(1600), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "mppa"
    )
    LaunchedEffect(Unit) {
        shown = true
        delay(350)
        glow.animateTo(0f, tween(1500))
    }
    AnimatedVisibility(
        visible = shown,
        enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = 260f)) { it } + fadeIn(tween(250))
    ) {
        LiquidGlass(
            s.glassAlpha(),
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val a = if (Opts.lite) 0f else maxOf(glow.value * 0.6f, if (s.playing) pulseA else 0f)
                    if (a > 0.01f) {
                        val padX = 36.dp.toPx()
                        val padY = 26.dp.toPx()
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                listOf(art.copy(alpha = a), Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.62f
                            ),
                            topLeft = Offset(-padX, -padY),
                            size = Size(size.width + padX * 2, size.height + padY * 2),
                            cornerRadius = CornerRadius(60.dp.toPx())
                        )
                    }
                }
                .softClick { s.showPlayer = true }
                .pointerInput(Unit) {
                    var dx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onDragEnd = { if (dx > 120f) s.prev() else if (dx < -120f) s.next() }
                    ) { _, d -> dx += d }
                },
            32.dp
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                    val ring = s.accent
                    val total = song.seconds.coerceAtLeast(1)
                    Canvas(Modifier.fillMaxSize()) {
                        val progress = (s.position / total).coerceIn(0f, 1f)
                        drawArc(
                            color = ring,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Cover(song, Modifier.size(46.dp).clip(CircleShape), 23.dp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (s.status.isNotEmpty()) s.status else song.artist,
                        color = TextDim,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                RoundBtn(Icons.Rounded.SkipPrevious, 0.9f, { s.prev() }, 42.dp)
                Spacer(Modifier.width(6.dp))
                PlayButton(s, 52.dp, 30.dp)
                Spacer(Modifier.width(6.dp))
                RoundBtn(Icons.Rounded.SkipNext, 0.9f, { s.next() }, 42.dp)
            }
        }
    }
}

@Composable
fun rememberArtColor(song: Song): Color {
    val ctx = LocalContext.current
    var color by remember(song.id) { mutableStateOf(song.color) }
    LaunchedEffect(song.id) {
        val t = song.thumb ?: return@LaunchedEffect
        try {
            val req = ImageRequest.Builder(ctx).data(artUrl(t, 200)).allowHardware(false).size(48).build()
            val res = ctx.imageLoader.execute(req)
            val bmp = (res as? SuccessResult)?.drawable?.toBitmap(1, 1)
            if (bmp != null) color = Color(bmp.getPixel(0, 0))
        } catch (e: Exception) {
        }
    }
    return color
}

@Composable
fun SeekBar(
    positionProvider: () -> Float,
    max: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    val h by animateDpAsState(if (dragging) 12.dp else 6.dp, spring(dampingRatio = 0.6f, stiffness = 400f), label = "seekh")
    Box(
        modifier
            .height(30.dp)
            .pointerInput(max) {
                detectTapGestures { off -> onSeek((off.x / size.width).coerceIn(0f, 1f) * max) }
            }
            .pointerInput(max) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                ) { change, _ ->
                    change.consume()
                    onSeek((change.position.x / size.width).coerceIn(0f, 1f) * max)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(h)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.28f))
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(h)
                .clip(RoundedCornerShape(50))
                .drawBehind {
                    val frac = (positionProvider() / max).coerceIn(0f, 1f)
                    drawRect(Color.White.copy(alpha = 0.92f), size = Size(size.width * frac, size.height))
                }
        )
    }
}

@Composable
private fun TimeLabels(s: AppState, total: Float) {
    val pos = s.position.coerceIn(0f, total)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(fmt(pos.toInt()), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        Text("-" + fmt((total - pos).toInt()), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
    }
}

@Composable
private fun PlayerIcon(icon: ImageVector, tint: Color, size: Dp = 26.dp, box: Dp = 48.dp, onClick: () -> Unit) {
    val c by animateColorAsState(tint, tween(250), label = "pit")
    val pop = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(tint) {
        if (first) {
            first = false
        } else {
            pop.animateTo(1.3f, tween(100))
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 350f))
        }
    }
    Box(Modifier.bounceClick(onClick).size(box), contentAlignment = Alignment.Center) {
        Icon(
            icon,
            contentDescription = null,
            tint = c,
            modifier = Modifier.size(size).graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
        )
    }
}

@Composable
private fun VolumeRow() {
    val ctx = LocalContext.current
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var vol by remember { mutableStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.VolumeDown, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
        SeekBar(
            positionProvider = { vol },
            max = maxVol.toFloat(),
            onSeek = {
                vol = it
                am.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
            },
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
        )
        Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun FullPlayer(s: AppState) {
    val song = s.current ?: return
    var sleepDialog by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    var menuDialog by remember { mutableStateOf(false) }
    var queueDialog by remember { mutableStateOf(false) }
    BackHandler { s.showPlayer = false }
    LaunchedEffect(song.id) { VideoSync.check(song) }
    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val lyShift by animateFloatAsState(
        if (Pages.open == "lyrics") 1f else 0f,
        tween(520, easing = FastOutSlowInEasing),
        label = "lyshift"
    )
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = lyShift * size.height * 0.18f
                alpha = 1f - lyShift * 0.9f
            }
            .offset { IntOffset(0, dragY.value.roundToInt()) }
            .draggable(
                state = rememberDraggableState { d ->
                    scope.launch { dragY.snapTo((dragY.value + d).coerceAtLeast(0f)) }
                },
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (dragY.value > 300f) s.showPlayer = false
                    else dragY.animateTo(0f, spring(dampingRatio = 0.7f))
                }
            )
            .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
            .background(Night)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
    ) {
        AnimatedContent(
            targetState = s.playerStyle,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "pstyle"
        ) { style ->
            if (style == 1) {
                ImmersiveBody(
                    s, song,
                    { menuDialog = true }, { addDialog = true }, { sleepDialog = true }, { queueDialog = true }
                )
            } else {
                AlbumBody(s, song, { menuDialog = true }, { addDialog = true }, { sleepDialog = true })
            }
        }
    }
    if (menuDialog) {
        PlayerMenuSheet(
            s, song,
            { menuDialog = false }, { addDialog = true }, { sleepDialog = true }, { queueDialog = true }
        )
    }
    if (addDialog) AddToPlaylistDialog(song, s) { addDialog = false }
    if (queueDialog) {
        LaunchedEffect(Unit) {
            Pages.show("queue")
            queueDialog = false
        }
    }
    if (sleepDialog) {
        LaunchedEffect(Unit) {
            Pages.show("sleep")
            sleepDialog = false
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleRow(
    s: AppState,
    song: Song,
    titleSize: TextUnit,
    artistSize: TextUnit,
    circles: Boolean,
    onMenu: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = Color.White,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
            Text(
                if (s.status.isNotEmpty()) s.status else song.artist,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = artistSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
        val liked = s.isLiked(song)
        val likePop = remember { Animatable(1f) }
        LaunchedEffect(liked) {
            if (liked) {
                likePop.animateTo(1.4f, tween(110))
                likePop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
            }
        }
        val bgMod = if (circles) Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.18f)) else Modifier
        Box(
            Modifier.bounceClick { s.toggleLike(song) }.size(40.dp).graphicsLayer {
                scaleX = likePop.value
                scaleY = likePop.value
            }.then(bgMod),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (circles) (if (liked) Icons.Rounded.Star else Icons.Rounded.StarBorder)
                else (if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder),
                contentDescription = null,
                tint = if (liked && !circles) s.accent else Color.White,
                modifier = Modifier.size(if (circles) 22.dp else 30.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.bounceClick(onMenu).size(40.dp).then(bgMod),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun NudgeIcon(icon: ImageVector, dir: Float, size: Dp, box: Dp, onClick: () -> Unit) {
    val nudge = remember { Animatable(0f) }
    val sc = rememberCoroutineScope()
    Box(
        Modifier
            .bounceClick {
                sc.launch {
                    nudge.animateTo(dir * 18f, tween(90))
                    nudge.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 400f))
                }
                onClick()
            }
            .size(box)
            .graphicsLayer { translationX = nudge.value },
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size)) }
}

@Composable
private fun TransportRow(s: AppState, side: Dp, big: Dp, skipIcons: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NudgeIcon(if (skipIcons) Icons.Rounded.SkipPrevious else Icons.Rounded.FastRewind, -1f, side, side + 28.dp) { s.prev() }
        Box(Modifier.bounceClick { s.toggle() }.size(big + 18.dp), contentAlignment = Alignment.Center) {
            if (s.loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 4.dp, modifier = Modifier.size(big))
            } else {
                AnimatedContent(
                    targetState = s.playing,
                    transitionSpec = {
                        (fadeIn(tween(160)) + scaleIn(initialScale = 0.5f)) togetherWith
                            (fadeOut(tween(100)) + scaleOut(targetScale = 0.5f))
                    },
                    label = "bigplay"
                ) { p ->
                    Icon(
                        if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(big)
                    )
                }
            }
        }
        NudgeIcon(if (skipIcons) Icons.Rounded.SkipNext else Icons.Rounded.FastForward, 1f, side, side + 28.dp) { s.next() }
    }
}

@Composable
private fun AlbumBody(s: AppState, song: Song, onMenu: () -> Unit, onAdd: () -> Unit, onSleep: () -> Unit) {
    val art = rememberArtColor(song)
    val c1 by animateColorAsState(lerp(art, Night, 0.15f), tween(700), label = "g1")
    val c2 by animateColorAsState(lerp(art, Night, 0.45f), tween(700), label = "g2")
    val c3 by animateColorAsState(lerp(art, Night, 0.80f), tween(700), label = "g3")
    val coverScale by animateFloatAsState(
        if (s.playing) 1f else 0.88f,
        spring(dampingRatio = 0.6f, stiffness = 260f),
        label = "cover"
    )
    val total = song.seconds.coerceAtLeast(1).toFloat()
    val coverShape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c1, c2, c3)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .padding(top = 10.dp, bottom = 20.dp)
                .width(44.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.45f))
                .clickable { s.showPlayer = false }
        )
        if (VideoSync.has[song.id] == true) {
            ModePills()
            Spacer(Modifier.height(14.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = coverScale
                    scaleY = coverScale
                }
                .shadow(22.dp, coverShape, ambientColor = art, spotColor = art)
        ) {
            if (VideoSync.showVideo(song)) {
                VideoStage(s, song, Modifier.fillMaxSize().clip(coverShape))
            } else {
                Cover(song, Modifier.fillMaxSize(), 20.dp, 96.sp, quality = 1000)
            }
        }
        Spacer(Modifier.height(26.dp))
        TitleRow(s, song, 22.sp, 19.sp, true, onMenu)
        Spacer(Modifier.weight(1f))
        SeekBar({ s.position }, total, { s.seekTo(it) }, Modifier.fillMaxWidth())
        TimeLabels(s, total)
        Spacer(Modifier.weight(1f))
        TransportRow(s, 44.dp, 78.dp)
        Spacer(Modifier.weight(1f))
        VolumeRow()
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerIcon(Icons.Rounded.Shuffle, if (s.shuffle) s.accent else Color.White.copy(alpha = 0.7f)) {
                s.shuffle = !s.shuffle
            }
            PlayerIcon(Icons.Rounded.Add, Color.White.copy(alpha = 0.7f)) { onAdd() }
            PlayerIcon(Icons.Rounded.Subtitles, Color.White.copy(alpha = 0.7f)) { Pages.show("lyrics") }
            PlayerIcon(Icons.Rounded.Bedtime, if (s.sleepLeft > 0) s.accent else Color.White.copy(alpha = 0.7f)) {
                onSleep()
            }
            PlayerIcon(Icons.Rounded.Repeat, if (s.repeat) s.accent else Color.White.copy(alpha = 0.7f)) {
                s.repeat = !s.repeat
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ImmersiveBody(
    s: AppState,
    song: Song,
    onMenu: () -> Unit,
    onAdd: () -> Unit,
    onSleep: () -> Unit,
    onQueue: () -> Unit
) {
    val art = rememberArtColor(song)
    val bottom by animateColorAsState(lerp(art, Color.Black, 0.55f), tween(700), label = "ibot")
    val total = song.seconds.coerceAtLeast(1).toFloat()
    val stageFade = remember {
        Brush.verticalGradient(
            0f to Color.Black,
            0.44f to Color.Black,
            0.58f to Color.Black.copy(alpha = 0.96f),
            0.70f to Color.Black.copy(alpha = 0.70f),
            0.82f to Color.Black.copy(alpha = 0.28f),
            1f to Color.Transparent
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Night)) {
        val stageHeight = maxHeight * 0.64f
        // blurred backdrop that fills the whole screen
        if (VideoSync.showVideo(song)) {
            VideoStage(s, song, Modifier.fillMaxSize().blur(50.dp), low = true)
        } else {
            Cover(song, Modifier.fillMaxSize().blur(50.dp), 0.dp, 160.sp, quality = 600)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.20f),
                    0.50f to Color.Black.copy(alpha = 0.30f),
                    1f to bottom.copy(alpha = 0.90f)
                )
            )
        )
        // sharp cover stage on top, fading into the backdrop
        Box(
            Modifier
                .fillMaxWidth()
                .height(stageHeight)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = stageFade, blendMode = BlendMode.DstIn)
                }
        ) {
            if (VideoSync.showVideo(song)) {
                VideoStage(s, song, Modifier.fillMaxSize())
            } else {
                Cover(song, Modifier.fillMaxSize(), 0.dp, 160.sp, quality = 1200)
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.22f to Color.Transparent
                    )
                )
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (VideoSync.has[song.id] == true) {
                Spacer(Modifier.height(10.dp))
                ModePills()
            } else {
                Text(
                    "Playing from PrismWave",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 18.dp).clickable { s.showPlayer = false }
                )
            }
            Spacer(Modifier.weight(1f))
            TitleRow(s, song, 27.sp, 19.sp, false, onMenu)
            Spacer(Modifier.height(18.dp))
            SeekBar({ s.position }, total, { s.seekTo(it) }, Modifier.fillMaxWidth())
            TimeLabels(s, total)
            Spacer(Modifier.height(6.dp))
            TransportRow(s, 50.dp, 72.dp, true)
            Spacer(Modifier.height(6.dp))
            VolumeRow()
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerIcon(Icons.Rounded.Shuffle, if (s.shuffle) s.accent else Color.White) {
                    s.shuffle = !s.shuffle
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerIcon(Icons.Rounded.Bedtime, if (s.sleepLeft > 0) s.accent else Color.White) { onSleep() }
                    Box(Modifier.width(1.dp).height(26.dp).background(Color.White.copy(alpha = 0.25f)))
                    PlayerIcon(Icons.Rounded.Add, Color.White) { onAdd() }
                    Box(Modifier.width(1.dp).height(26.dp).background(Color.White.copy(alpha = 0.25f)))
                    PlayerIcon(Icons.Rounded.Subtitles, Color.White) { Pages.show("lyrics") }
                    Box(Modifier.width(1.dp).height(26.dp).background(Color.White.copy(alpha = 0.25f)))
                    PlayerIcon(Icons.Rounded.Repeat, if (s.repeat) s.accent else Color.White) {
                        s.repeat = !s.repeat
                    }
                }
                PlayerIcon(Icons.Rounded.FormatListBulleted, Color.White) { onQueue() }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun PlayButton(s: AppState, size: Dp, iconSize: Dp) {
    val corner by animateDpAsState(
        if (s.playing) size / 3 else size / 2,
        spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "morph"
    )
    Box(
        Modifier
            .bounceClick { s.toggle() }
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(s.accent),
        contentAlignment = Alignment.Center
    ) {
        if (s.loading) {
            CircularProgressIndicator(color = Color.Black, strokeWidth = 3.dp, modifier = Modifier.size(iconSize))
        } else {
            AnimatedContent(
                targetState = s.playing,
                transitionSpec = {
                    (fadeIn(tween(160)) + scaleIn(initialScale = 0.5f)) togetherWith
                        (fadeOut(tween(100)) + scaleOut(targetScale = 0.5f))
                },
                label = "playicon"
            ) { playing ->
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}


// ------------------------------------------------------------ blurred strip under the status bar

@Composable
fun TopBlur(height: Dp) {
    if (!Opts.statusBlur || Opts.lite) return
    val backdrop = LocalBackdrop.current
    val useBlur = backdrop != null && backdrop.enabled && Build.VERSION.SDK_INT >= 31
    val blurLayer = rememberGraphicsLayer()
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(0f to Color.Black, 0.6f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
            .drawBehind {
                if (useBlur && backdrop != null && backdrop.size.width > 0) {
                    blurLayer.renderEffect = BlurEffect(backdrop.radius * Opts.blurAmount, backdrop.radius * Opts.blurAmount, TileMode.Clamp)
                    blurLayer.clip = true
                    blurLayer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
                        drawLayer(backdrop.layer)
                    }
                    drawLayer(blurLayer)
                }
                drawRect(Night.copy(alpha = if (useBlur) Opts.blurDark else 0.85f))
            }
    )
}

// ------------------------------------------------------------ player options sheet

private class MenuAction(val icon: ImageVector, val label: String, val on: Boolean, val run: () -> Unit)

@Composable
private fun MenuTile(a: MenuAction, accent: Color, modifier: Modifier) {
    Column(
        modifier
            .height(96.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg2)
            .softClick(a.run)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(a.icon, contentDescription = null, tint = if (a.on) accent else Color.White.copy(alpha = 0.85f), modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(a.label, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, sub: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(18.dp))
        Column {
            Text(label, color = Color.White, fontSize = 18.sp)
            if (sub != null) Text(sub, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun VolumeCard(accent: Color) {
    val ctx = LocalContext.current
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var vol by remember { mutableStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(CardBg).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Volume", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text((vol / maxVol * 100).toInt().toString() + "%", color = accent, fontSize = 18.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(CardBg2).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.VolumeDown, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
            SeekBar(
                positionProvider = { vol },
                max = maxVol.toFloat(),
                onSeek = {
                    vol = it
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
                },
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun PlayerMenuSheet(
    s: AppState,
    song: Song,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onSleep: () -> Unit,
    onQueue: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val appScope = remember {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
    }
    var visible by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var tempoOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val close: () -> Unit = {
        scope.launch {
            visible = false
            delay(230)
            onDismiss()
        }
    }
    BackHandler { close() }
    fun toast(t: String) = Toast.makeText(ctx, t, Toast.LENGTH_SHORT).show()
    val liked = s.isLiked(song)
    val actions = listOf(
        MenuAction(Icons.Rounded.Bedtime, "Sleep timer", s.sleepLeft > 0) { close(); onSleep() },
        MenuAction(Icons.Rounded.Radio, "Start radio", false) {
            close()
            toast("Starting radio")
            appScope.launch {
                try {
                    val rel = try {
                        Engine.related(song.url)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val r = if (rel.size >= 5) rel else Engine.search(song.artist + " " + song.title)
                    s.replaceQueue(listOf(song) + r.filter { it.id != song.id })
                    s.radio = true
                    toast("Radio ready")
                } catch (e: Exception) {
                    toast("Radio failed, check your connection")
                }
            }
        },
        MenuAction(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (liked) "Unlike" else "Like", liked) {
            s.toggleLike(song)
        },
        MenuAction(Icons.Rounded.PlaylistAdd, "Add to playlist", false) { close(); onAdd() },
        MenuAction(Icons.Rounded.ContentCopy, "Copy link", false) {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("link", song.url))
            toast("Link copied")
        },
        MenuAction(Icons.Rounded.QueueMusic, "Queue", false) { close(); onQueue() },
        MenuAction(Icons.Rounded.Share, "Share", false) {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, song.title + " - " + song.artist + "\n" + song.url)
            }
            ctx.startActivity(Intent.createChooser(i, null))
        },
        MenuAction(Icons.Rounded.Subtitles, "Lyrics", false) { close(); Pages.show("lyrics") },
        MenuAction(Icons.Rounded.DirectionsCar, "Car mode", false) { close(); Pages.show("car") }
    )
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.92f).dp
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { close() }
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) { it },
            exit = slideOutVertically(tween(220)) { it }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxH)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Night)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    Modifier
                        .padding(vertical = 14.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                )
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(CardBg).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cover(song, Modifier.size(72.dp), 16.dp, 24.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Now Playing", color = s.accent, fontSize = 17.sp)
                        Text(
                            song.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(song.artist, color = Color.White.copy(alpha = 0.75f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(12.dp))
                VolumeCard(s.accent)
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(CardBg).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    actions.chunked(3).forEach { rowItems ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { a -> MenuTile(a, s.accent, Modifier.weight(1f)) }
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(CardBg)) {
                    MenuRow(Icons.Rounded.Person, "View artist", song.artist) { Pages.openCollection(song.artist, "", 3) }
                    Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                    val isDown = DownloadStore.isDownloaded(song)
                    val dlProg = DownloadStore.progress[song.id]
                    MenuRow(
                        Icons.Rounded.Download,
                        if (isDown) "Remove download" else if (dlProg != null) "Downloading " + (dlProg * 100).toInt() + "%" else "Download"
                    ) {
                        if (isDown) {
                            DownloadStore.remove(ctx, song)
                            toast("Download removed")
                        } else if (dlProg == null) {
                            DownloadStore.start(ctx, song)
                            toast("Downloading")
                        }
                    }
                    Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                    MenuRow(Icons.Rounded.Info, "Details") { details = true }
                    Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                    MenuRow(Icons.Rounded.Equalizer, "Equalizer") { Pages.show("eq") }
                    Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                    MenuRow(
                        Icons.Rounded.Speed,
                        "Tempo and Pitch",
                        "x" + "%.2f".format(s.tempo) + "  •  x" + "%.2f".format(s.pitch)
                    ) { tempoOpen = true }
                }
            }
        }
    }
    if (tempoOpen) TempoPitchDialog(s) { tempoOpen = false }
    if (details) {
        LaunchedEffect(Unit) {
            Pages.show("details", song)
            details = false
        }
    }
}
