package com.prismwave.app

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

fun fmt(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

fun fmtLong(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return if (h > 0) h.toString() + "h " + m + "m" else m.toString() + "m"
}

private val sizeWh = Regex("=w\\d+-h\\d+")
private val sizeS = Regex("=s\\d+")
private val ytThumb = Regex("/(default|mqdefault|hqdefault|sddefault)\\.jpg")

/** Asks YouTube/Google for a sharper version of the same artwork. */
fun artUrl(url: String, px: Int): String = when {
    "googleusercontent.com" in url || "ggpht.com" in url -> when {
        sizeWh.containsMatchIn(url) -> sizeWh.replace(url, "=w" + px + "-h" + px)
        sizeS.containsMatchIn(url) -> sizeS.replace(url, "=s" + px)
        else -> url
    }
    "i.ytimg.com" in url && px >= 700 -> ytThumb.replace(url, "/hq720.jpg")
    else -> url
}

/** Spring squish + tick on tap (Material expressive feel). */
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val cb by rememberUpdatedState(onClick)
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "bounce"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    cb()
                }
            )
        }
}

/** Fade + rise-in with a small stagger. */
fun Modifier.appear(index: Int): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index * 60L).coerceAtMost(420L))
        a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 200f))
    }
    this.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 60f
    }
}

/** Holds a recorded copy of the screen content so bars can draw a blurred version of it. */
class Backdrop(val layer: GraphicsLayer) {
    var size by mutableStateOf(IntSize.Zero)
    var radius by mutableStateOf(48f)
}

val LocalBackdrop = compositionLocalOf<Backdrop?> { null }

@Composable
fun PLogo(size: Dp, color: Color = Gold) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text("P", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.55f).sp)
    }
}

@Composable
fun Cover(
    song: Song,
    modifier: Modifier = Modifier,
    radius: Dp = 14.dp,
    letterSize: TextUnit = 22.sp,
    quality: Int = 400
) {
    val ctx = LocalContext.current
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(listOf(song.color, Night))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            song.title.take(1),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = letterSize,
            fontWeight = FontWeight.Bold
        )
        val thumb = song.thumb
        if (thumb != null) {
            val req = remember(thumb, quality) {
                ImageRequest.Builder(ctx).data(artUrl(thumb, quality)).crossfade(350).build()
            }
            AsyncImage(
                model = req,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun Glass(
    alpha: Float,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val backdrop = LocalBackdrop.current
    val useBlur = backdrop != null && Build.VERSION.SDK_INT >= 31 && alpha < 0.999f
    val blurLayer = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .clip(shape)
            .then(modifier)
            .onGloballyPositioned { pos = it.positionInRoot() }
            .drawBehind {
                if (useBlur && backdrop != null && backdrop.size.width > 0) {
                    blurLayer.renderEffect = BlurEffect(backdrop.radius, backdrop.radius, TileMode.Clamp)
                    blurLayer.record(this, layoutDirection, backdrop.size) {
                        drawLayer(backdrop.layer)
                    }
                    translate(-pos.x, -pos.y) { drawLayer(blurLayer) }
                } else {
                    drawRect(Night)
                }
            }
            .background(Surface2.copy(alpha = if (useBlur) alpha * 0.6f else alpha))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape),
        content = content
    )
}

@Composable
fun RoundBtn(
    icon: ImageVector,
    alpha: Float,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    tint: Color = Color.White
) {
    Box(
        Modifier
            .bounceClick(onClick)
            .size(size)
            .clip(CircleShape)
            .background(Surface2.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, accent: Color, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .bounceClick(onClick)
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else Surface2.copy(alpha = 0.8f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.Black else Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (selected) Color.Black else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SectionTitle(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, color = Gold, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable
fun PlayBadge(accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(34.dp).clip(CircleShape).background(accent),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun NamePlaylistDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Playlist name", color = TextDim) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Gold,
                    cursorColor = Gold
                )
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddToPlaylistDialog(song: Song, s: AppState, onDone: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        NamePlaylistDialog("New playlist", "", { n ->
            val id = s.createPlaylist(n)
            s.addToPlaylist(id, song)
            onDone()
        }, { creating = false })
        return
    }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Add to playlist") },
        text = {
            Column {
                Text(
                    "+ New playlist",
                    color = Gold,
                    modifier = Modifier.fillMaxWidth().clickable { creating = true }.padding(vertical = 12.dp)
                )
                s.playlists.forEach { p ->
                    Text(
                        p.name + "  (" + p.songs.size + ")",
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                s.addToPlaylist(p.id, song)
                                onDone()
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text("Close") } }
    )
}

@Composable
fun SongMenu(song: Song, s: AppState, playlistId: String?, onDismiss: () -> Unit) {
    var pick by remember { mutableStateOf(false) }
    if (pick) {
        AddToPlaylistDialog(song, s) { pick = false; onDismiss() }
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                val items = buildList<Pair<String, () -> Unit>> {
                    add((if (s.isLiked(song)) "Remove from liked" else "Like") to { s.toggleLike(song); onDismiss() })
                    add("Add to playlist" to { pick = true })
                    if (playlistId != null) {
                        add("Remove from this playlist" to { s.removeFromPlaylist(playlistId, song.id); onDismiss() })
                    }
                }
                items.forEach { (label, action) ->
                    Text(
                        label,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun SongRow(song: Song, s: AppState, from: List<Song> = listOf(song), playlistId: String? = null) {
    val isCurrent = s.current?.id == song.id
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { s.play(song, from) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(song, Modifier.size(52.dp), 12.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) s.accent else Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist + "  •  " + fmt(song.seconds),
                color = TextDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = null,
            tint = TextDim,
            modifier = Modifier.size(28.dp).clickable { menu = true }
        )
    }
    if (menu) SongMenu(song, s, playlistId) { menu = false }
}
