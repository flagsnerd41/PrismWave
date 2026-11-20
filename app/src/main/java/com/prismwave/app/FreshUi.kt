package com.prismwave.app

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------ pinned header with a fading blur

// height of the Home / Search / Library header row (14 dp padding, 46 dp buttons, 14 dp padding)
val PinnedHeaderHeight = 74.dp

// the header stays on top while Home, Search and the Library main page scroll under it
fun headerShown(s: AppState): Boolean =
    s.screen == Screen.Home ||
        s.screen == Screen.Search ||
        (s.screen == Screen.Library && s.openPlaylist == null && s.libraryOpen == null)

// scrolling down hides the header, scrolling back up shows it again
object HeaderScroll {
    var hidden by androidx.compose.runtime.mutableStateOf(false)
    val connection = object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
        ): Offset {
            if (available.y < -6f) hidden = true else if (available.y > 6f) hidden = false
            return Offset.Zero
        }
    }
}

@Composable
fun PinnedHeader(s: AppState) {
    val top = statusTop()
    LaunchedEffect(s.screen) { HeaderScroll.hidden = false }
    val p by androidx.compose.animation.core.animateFloatAsState(
        if (HeaderScroll.hidden) 1f else 0f,
        androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "hdrHide"
    )
    val total = top + PinnedHeaderHeight + 28.dp
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = -total.toPx() * p
                alpha = 1f - p
            }
    ) {
        FadeBlurBar(total)
        Column(Modifier.padding(top = top)) { Header(s) }
    }
}

// a blurred copy of the screen that is strong at the top and fades to nothing at the bottom
@Composable
fun FadeBlurBar(height: Dp, modifier: Modifier = Modifier) {
    val backdrop = LocalBackdrop.current
    val useBlur = backdrop != null && backdrop.enabled && Build.VERSION.SDK_INT >= 31 && Opts.statusBlur
    val blurLayer = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .onGloballyPositioned { pos = it.positionInRoot() }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                if (useBlur && backdrop != null && backdrop.size.width > 0) {
                    // same trick as the glass bars: blur a quarter-size copy and scale it back up
                    val k = 0.25f
                    val r = (backdrop.radius * 0.5f * k).coerceAtLeast(1f)
                    blurLayer.renderEffect = BlurEffect(r, r, TileMode.Clamp)
                    blurLayer.clip = true
                    blurLayer.record(
                        this, layoutDirection,
                        IntSize((size.width * k).toInt().coerceAtLeast(1), (size.height * k).toInt().coerceAtLeast(1))
                    ) {
                        scale(k, k, Offset.Zero) { translate(-pos.x, -pos.y) { drawLayer(backdrop.layer) } }
                    }
                    scale(1f / k, 1f / k, Offset.Zero) { drawLayer(blurLayer) }
                    drawRect(Night.copy(alpha = 0.40f))
                } else {
                    drawRect(Night.copy(alpha = 0.80f))
                }
                // fade the whole thing out towards the bottom
                drawRect(
                    Brush.verticalGradient(0f to Color.Black, 0.6f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
    )
}

// ------------------------------------------------------------ playlist page, ArchiveTune style

@Composable
private fun PlainCircle(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .bounceClick(onClick)
            .size(46.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = Color.White) }
}

@Composable
private fun RoundAction(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .bounceClick(onClick)
            .size(54.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) { content() }
}

// slides (and fades / scales) its content in once, with a delay, used by the playlist page
@Composable
private fun PlEnter(
    delayMs: Int,
    dx: Float,
    dy: Float,
    scaleFrom: Float = 1f,
    content: @Composable () -> Unit
) {
    val p = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        p.animateTo(1f, androidx.compose.animation.core.tween(560, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    Box(
        Modifier.graphicsLayer {
            val k = 1f - p.value
            translationX = dx * k * density
            translationY = dy * k * density
            alpha = p.value
            val sc = scaleFrom + (1f - scaleFrom) * p.value
            scaleX = sc
            scaleY = sc
        }
    ) { content() }
}

@Composable
fun PlaylistScreen2(s: AppState, id: String) {
    val pl = s.playlists.find { it.id == id }
    if (pl == null) {
        LaunchedEffect(Unit) { s.openPlaylist = null }
        return
    }
    val ctx = LocalContext.current
    var unlocked by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val first = pl.songs.firstOrNull()
    val art = if (first != null) rememberArtColor(first) else s.accent
    val bg by animateColorAsState(lerp(Night, art, 0.16f), tween(600), label = "plbg")
    val allDown = pl.songs.isNotEmpty() && pl.songs.all { DownloadStore.isDownloaded(it) }
    val busy = pl.songs.any { DownloadStore.progress.containsKey(it.id) }
    val statusPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize().background(bg)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 220.dp)) {
            // new look: blurred cover backdrop, big rounded cover card and the title, all sliding in
            item {
                Box(Modifier.fillMaxWidth()) {
                    if (first != null) {
                        Box(Modifier.fillMaxWidth().height(430.dp).graphicsLayer { alpha = 0.55f }) {
                            Cover(first, Modifier.fillMaxSize().blur(46.dp), 0.dp, 120.sp, quality = 600)
                        }
                    }
                    Box(
                        Modifier.fillMaxWidth().height(430.dp).background(
                            Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.30f), 0.6f to bg.copy(alpha = 0.55f), 1f to bg)
                        )
                    )
                    Column(
                        Modifier.fillMaxWidth().padding(top = statusPad),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlainCircle(Icons.Rounded.ArrowBack) { s.openPlaylist = null }
                            Spacer(Modifier.weight(1f))
                            PlainCircle(Icons.Rounded.MoreVert) { menu = true }
                        }
                        Spacer(Modifier.height(6.dp))
                        // the cover slides in from the left side (where the library tile was) and settles
                        PlEnter(0, -90f, 0f, scaleFrom = 0.86f) {
                            Box(Modifier.size(250.dp).clip(RoundedCornerShape(30.dp))) {
                                if (first != null) {
                                    Cover(first, Modifier.fillMaxSize(), 0.dp, 120.sp, quality = 1000)
                                } else {
                                    Box(Modifier.fillMaxSize().background(CardBg2))
                                }
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        PlEnter(90, -60f, 0f) {
                            Text(
                                pl.name,
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 28.dp)
                            )
                        }
                        PlEnter(170, -40f, 0f) {
                            Text(
                                pl.songs.size.toString() + " tracks  \u2022  " + fmt(pl.songs.sumOf { it.seconds }),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
            // custom order, shuffle, big Play pill, download
            item {
              PlEnter(240, 0f, 40f) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundAction({ unlocked = !unlocked }) {
                        Icon(
                            if (unlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (unlocked) s.accent else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    RoundAction({
                        if (pl.songs.isNotEmpty()) {
                            s.shuffle = true
                            s.play(pl.songs.random(), pl.songs)
                        }
                    }) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(
                        Modifier
                            .bounceClick { if (pl.songs.isNotEmpty()) s.play(pl.songs[0], pl.songs) }
                            .height(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .padding(horizontal = 26.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Play", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    RoundAction({
                        if (allDown) {
                            Toast.makeText(ctx, "All songs are downloaded. Remove them from a song's menu.", Toast.LENGTH_SHORT).show()
                        } else {
                            DownloadStore.startAll(ctx, pl.songs)
                        }
                    }) {
                        when {
                            busy -> CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            allDown -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = s.accent, modifier = Modifier.size(26.dp))
                            else -> Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
              }
            }
            item { Spacer(Modifier.height(14.dp)) }
            if (pl.songs.isEmpty()) {
                item {
                    Text(
                        "Empty. Open any song's menu and tap Add to playlist.",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
            itemsIndexed(pl.songs, key = { _, sg -> sg.id }) { i, song ->
                val cur = s.current?.id == song.id
                val down = DownloadStore.isDownloaded(song)
                var rowMenu by remember { mutableStateOf(false) }
                Column(Modifier.animateItem().padding(horizontal = 14.dp, vertical = 3.dp)) {
                  PlEnter(300 + minOf(i, 8) * 45, -50f, 0f) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (cur) s.accent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f))
                            .clickable { s.play(song, pl.songs) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Cover(song, Modifier.size(58.dp), 14.dp, 18.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                song.title,
                                color = if (cur) s.accent else Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Public, contentDescription = null, tint = TextDim, modifier = Modifier.size(16.dp))
                                if (down) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = s.accent, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    song.artist + "  \u2022  " + fmt(song.seconds),
                                    color = TextDim,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (unlocked) {
                            Icon(
                                Icons.Rounded.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp).clickable { s.moveInPlaylist(pl.id, i, -1) }
                            )
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp).clickable { s.moveInPlaylist(pl.id, i, 1) }
                            )
                        } else {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp).clickable { rowMenu = true }
                            )
                        }
                    }
                  }
                }
                if (rowMenu) SongMenu(song, s, pl.id) { rowMenu = false }
            }
        }
    }
    if (menu) {
        LaunchedEffect(Unit) {
            Pages.show("plmenu", null, pl.id)
            menu = false
        }
    }
}
