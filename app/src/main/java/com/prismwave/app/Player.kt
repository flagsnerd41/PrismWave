package com.prismwave.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.Bedtime
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NavBar(s: AppState) {
    val items = listOf(
        Screen.Home to Icons.Rounded.Home,
        Screen.Search to Icons.Rounded.Search,
        Screen.Library to Icons.Rounded.LibraryMusic,
        Screen.Settings to Icons.Rounded.Settings
    )
    Glass(s.glassAlpha(), Modifier.fillMaxWidth().height(64.dp), RoundedCornerShape(32.dp)) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (screen, icon) ->
                val sel = s.screen == screen
                Row(
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (sel) s.accent.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { s.screen = screen }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = if (sel) s.accent else TextDim)
                    if (sel) {
                        Spacer(Modifier.width(6.dp))
                        Text(screen.name, color = s.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
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
        RoundedCornerShape(26.dp)
    ) {
        Column {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(song, Modifier.size(46.dp), 14.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(song.artist, color = TextDim, fontSize = 13.sp, maxLines = 1)
                }
                RoundBtn(
                    if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    0f,
                    { s.toggle() },
                    42.dp
                )
                RoundBtn(Icons.Rounded.SkipNext, 0f, { s.next() }, 42.dp)
            }
            LinearProgressIndicator(
                progress = { (s.position / song.seconds).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = s.accent,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun FullPlayer(s: AppState) {
    val song = s.current ?: return
    var sleepDialog by remember { mutableStateOf(false) }
    BackHandler { s.showPlayer = false }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(song.color.copy(alpha = 0.55f), Night, Night)))
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
            RoundBtn(
                Icons.Rounded.Bedtime,
                0.4f,
                { sleepDialog = true },
                tint = if (s.sleepLeft > 0) s.accent else Color.White
            )
        }
        Spacer(Modifier.height(12.dp))
        Cover(song, Modifier.fillMaxWidth().aspectRatio(1f), 28.dp, 96.sp)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(song.artist, color = TextDim, fontSize = 16.sp, maxLines = 1)
            }
            val liked = song.id in s.liked
            Icon(
                imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (liked) s.accent else Color.White,
                modifier = Modifier.size(30.dp).clickable { s.toggleLike(song) }
            )
        }
        Spacer(Modifier.height(12.dp))
        Slider(
            value = s.position.coerceIn(0f, song.seconds.toFloat()),
            onValueChange = { s.position = it },
            valueRange = 0f..song.seconds.toFloat()
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
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(s.accent).clickable { s.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }
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
