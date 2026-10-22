package com.prismwave.app

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
    Glass(s.glassAlpha(), Modifier.fillMaxWidth().height(78.dp), RoundedCornerShape(30.dp)) {
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
    Glass(
        s.glassAlpha(),
        Modifier.fillMaxWidth().softClick { s.showPlayer = true },
        RoundedCornerShape(32.dp)
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
    Box(Modifier.bounceClick(onClick).size(box), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
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
    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxSize()
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
    if (menuDialog) SongMenu(song, s, null) { menuDialog = false }
    if (addDialog) AddToPlaylistDialog(song, s) { addDialog = false }
    if (queueDialog) {
        AlertDialog(
            onDismissRequest = { queueDialog = false },
            confirmButton = { TextButton(onClick = { queueDialog = false }) { Text("Close") } },
            title = { Text("Queue") },
            text = {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(s.queue.take(100)) { q ->
                        Row(
                            Modifier.fillMaxWidth().clickable { s.play(q, s.queue) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Cover(q, Modifier.size(42.dp), 10.dp, 16.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    q.title,
                                    color = if (q.id == song.id) s.accent else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(q.artist, color = TextDim, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        )
    }
    if (sleepDialog) {
        AlertDialog(
            onDismissRequest = { sleepDialog = false },
            confirmButton = { TextButton(onClick = { sleepDialog = false }) { Text("Close") } },
            title = { Text("Sleep timer") },
            text = {
                Column {
                    listOf(0 to "Off", 15 to "15 minutes", 30 to "30 minutes", 60 to "60 minutes").forEach { (m, label) ->
                        Text(
                            label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    s.sleepLeft = m * 60
                                    sleepDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        )
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
        val bgMod = if (circles) Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.18f)) else Modifier
        Box(
            Modifier.bounceClick { s.toggleLike(song) }.size(40.dp).then(bgMod),
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
private fun TransportRow(s: AppState, side: Dp, big: Dp, skipIcons: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerIcon(if (skipIcons) Icons.Rounded.SkipPrevious else Icons.Rounded.FastRewind, Color.White, side, side + 28.dp) { s.prev() }
        Box(Modifier.bounceClick { s.toggle() }.size(big + 18.dp), contentAlignment = Alignment.Center) {
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
        PlayerIcon(if (skipIcons) Icons.Rounded.SkipNext else Icons.Rounded.FastForward, Color.White, side, side + 28.dp) { s.next() }
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
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = coverScale
                    scaleY = coverScale
                }
                .shadow(22.dp, coverShape)
        ) { Cover(song, Modifier.fillMaxSize(), 20.dp, 96.sp, quality = 1000) }
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
        Cover(song, Modifier.fillMaxSize().blur(50.dp), 0.dp, 160.sp, quality = 600)
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
            Cover(song, Modifier.fillMaxSize(), 0.dp, 160.sp, quality = 1200)
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
            Text(
                "Playing from PrismWave",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 18.dp).clickable { s.showPlayer = false }
            )
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
