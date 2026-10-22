package com.prismwave.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
                        .clickable {
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
        Modifier.fillMaxWidth().clickable { s.showPlayer = true },
        RoundedCornerShape(32.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                val progress = (s.position / song.seconds.coerceAtLeast(1)).coerceIn(0f, 1f)
                val ring = s.accent
                Canvas(Modifier.fillMaxSize()) {
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
fun FullPlayer(s: AppState) {
    val song = s.current ?: return
    var sleepDialog by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    BackHandler { s.showPlayer = false }
    val bg by animateColorAsState(song.color.copy(alpha = 0.55f), tween(700), label = "bg")
    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Column(
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
            .background(Brush.verticalGradient(listOf(bg, Night, Night)))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundBtn(Icons.Rounded.KeyboardArrowDown, 0.4f, { s.showPlayer = false })
            Text(
                "Now playing",
                color = TextDim,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            RoundBtn(Icons.Rounded.Add, 0.4f, { addDialog = true })
            Spacer(Modifier.width(8.dp))
            RoundBtn(
                Icons.Rounded.Bedtime,
                0.4f,
                { sleepDialog = true },
                tint = if (s.sleepLeft > 0) s.accent else Color.White
            )
        }
        Spacer(Modifier.height(12.dp))
        Cover(song, Modifier.fillMaxWidth().aspectRatio(1f), 28.dp, 96.sp, quality = 1000)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    if (s.status.isNotEmpty()) s.status else song.artist,
                    color = TextDim,
                    fontSize = 16.sp,
                    maxLines = 1
                )
            }
            val liked = s.isLiked(song)
            Icon(
                imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (liked) s.accent else Color.White,
                modifier = Modifier.size(30.dp).clickable { s.toggleLike(song) }
            )
        }
        Spacer(Modifier.height(12.dp))
        val total = song.seconds.coerceAtLeast(1).toFloat()
        Slider(
            value = s.position.coerceIn(0f, total),
            onValueChange = { s.seekTo(it) },
            valueRange = 0f..total
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(s.position.toInt()), color = TextDim, fontSize = 13.sp)
            Text(fmt(song.seconds), color = TextDim, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = null,
                tint = if (s.shuffle) s.accent else TextDim,
                modifier = Modifier.size(28.dp).clickable { s.shuffle = !s.shuffle }
            )
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp).clickable { s.prev() }
            )
            PlayButton(s, 76.dp, 42.dp)
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp).clickable { s.next() }
            )
            Icon(
                Icons.Rounded.Repeat,
                contentDescription = null,
                tint = if (s.repeat) s.accent else TextDim,
                modifier = Modifier.size(28.dp).clickable { s.repeat = !s.repeat }
            )
        }
    }
    if (addDialog) AddToPlaylistDialog(song, s) { addDialog = false }
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
