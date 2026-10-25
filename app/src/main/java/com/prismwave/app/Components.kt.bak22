package com.prismwave.app

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import android.widget.Toast

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
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
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = pressable(0.88f, onClick)

/** Gentler press effect for rows and big cards. */
fun Modifier.softClick(onClick: () -> Unit): Modifier = pressable(0.96f, onClick)

private fun Modifier.pressable(scaleTo: Float, onClick: () -> Unit): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val cb by rememberUpdatedState(onClick)
    val scale by animateFloatAsState(
        targetValue = if (pressed && Opts.bounce) scaleTo else 1f,
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
                    if (Opts.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    cb()
                }
            )
        }
}

val LocalReduceMotion = compositionLocalOf { false }

/** Fade + rise-in with a small stagger (skipped when Reduce motion is on). */
fun Modifier.appear(index: Int): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val a = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) {
            delay((index * 60L).coerceAtMost(420L))
            a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 200f))
        }
    }
    this.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 60f
    }
}

/** Holds a recorded copy of the screen content so bars can draw a blurred version of it. */
class Backdrop(val layer: GraphicsLayer) {
    var size by mutableStateOf(IntSize.Zero)
    var radius by mutableStateOf(80f)
    var enabled by mutableStateOf(true)
}

val LocalBackdrop = compositionLocalOf<Backdrop?> { null }

@Composable
fun AppLogo(size: Dp) {
    Image(
        painter = painterResource(R.drawable.app_logo),
        contentDescription = null,
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.24f))
    )
}

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
    val useBlur = backdrop != null && backdrop.enabled && Build.VERSION.SDK_INT >= 31 && alpha < 0.999f
    val blurLayer = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .clip(shape)
            .then(modifier)
            .onGloballyPositioned { pos = it.positionInRoot() }
            .drawBehind {
                if (useBlur && backdrop != null && backdrop.size.width > 0) {
                    // Only the bar's own area is blurred (much cheaper than the whole screen).
                    blurLayer.renderEffect = BlurEffect(backdrop.radius, backdrop.radius, TileMode.Clamp)
                    blurLayer.clip = true
                    blurLayer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
                        translate(-pos.x, -pos.y) { drawLayer(backdrop.layer) }
                    }
                    drawLayer(blurLayer)
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
            .background(Color.White.copy(alpha = 0.04f + alpha * 0.1f)),
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
            .background(if (selected) accent else Color.White.copy(alpha = 0.10f))
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
        Text(text, color = Color.White, fontFamily = PrismFont, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
    LaunchedEffect(Unit) { Pages.askName(title, initial, onConfirm, onDismiss) }
}

@Composable
fun AddToPlaylistDialog(song: Song, s: AppState, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        Pages.show("add", song)
        onDone()
    }
}

@Composable
fun SongMenu(song: Song, s: AppState, playlistId: String?, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        Pages.show("songmenu", song, playlistId)
        onDismiss()
    }
}

@Composable
fun SongRow(song: Song, s: AppState, from: List<Song> = listOf(song), playlistId: String? = null) {
    val isCurrent = s.current?.id == song.id
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .softClick { s.play(song, from) }
            .pointerInput(song.id) {
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onDragEnd = {
                        if (dx > 140f) {
                            s.toggleLike(song)
                            Toast.makeText(ctx, "Like updated", Toast.LENGTH_SHORT).show()
                        } else if (dx < -140f) {
                            s.queue = s.queue + song
                            Toast.makeText(ctx, "Added to queue", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { _, d -> dx += d }
            }
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
