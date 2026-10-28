package com.prismwave.app

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalView

import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------ extra settings (saved on the phone)

object Opts {
    private var sp: android.content.SharedPreferences? = null
    var haptics by mutableStateOf(true)
    var bounce by mutableStateOf(true)
    var statusBlur by mutableStateOf(true)
    var livingBg by mutableStateOf(true)
    var showKeep by mutableStateOf(true)
    var showSpeed by mutableStateOf(true)
    var showForgotten by mutableStateOf(true)
    var showMixes by mutableStateOf(true)
    var blurAmount by mutableStateOf(1f)
    var blurDark by mutableStateOf(0.35f)
    var cacheMb by mutableStateOf(512)
    var preload by mutableStateOf(true)
    var fade by mutableStateOf(true)
    var offline by mutableStateOf(false)
    var headphones by mutableStateOf(true)
    var crossfade by mutableStateOf(0)
    var eqOn by mutableStateOf(false)
    var eqLevels by mutableStateOf("")
    var bassBoost by mutableStateOf(0)
    var theme by mutableStateOf(0)
    var lite by mutableStateOf(false)

    fun init(c: Context) {
        val p = c.getSharedPreferences("prismwave_opts", Context.MODE_PRIVATE)
        sp = p
        haptics = p.getBoolean("haptics", true)
        bounce = p.getBoolean("bounce", true)
        statusBlur = p.getBoolean("statusBlur", true)
        livingBg = p.getBoolean("livingBg", true)
        showKeep = p.getBoolean("showKeep", true)
        showSpeed = p.getBoolean("showSpeed", true)
        showForgotten = p.getBoolean("showForgotten", true)
        showMixes = p.getBoolean("showMixes", true)
        blurAmount = p.getFloat("blurAmount", 1f)
        blurDark = p.getFloat("blurDark", 0.35f)
        cacheMb = p.getInt("cacheMb", 512)
        preload = p.getBoolean("preload", true)
        fade = p.getBoolean("fade", true)
        offline = p.getBoolean("offline", false)
        headphones = p.getBoolean("headphones", true)
        crossfade = p.getInt("crossfade", 0)
        eqOn = p.getBoolean("eqOn", false)
        eqLevels = p.getString("eqLevels", "") ?: ""
        bassBoost = p.getInt("bassBoost", 0)
        theme = p.getInt("theme", 0)
        lite = p.getBoolean("lite", false)
    }

    fun save() {
        sp?.edit()
            ?.putBoolean("haptics", haptics)
            ?.putBoolean("bounce", bounce)
            ?.putBoolean("statusBlur", statusBlur)
            ?.putBoolean("livingBg", livingBg)
            ?.putBoolean("showKeep", showKeep)
            ?.putBoolean("showSpeed", showSpeed)
            ?.putBoolean("showForgotten", showForgotten)
            ?.putBoolean("showMixes", showMixes)
            ?.putFloat("blurAmount", blurAmount)
            ?.putFloat("blurDark", blurDark)
            ?.putInt("cacheMb", cacheMb)
            ?.putBoolean("preload", preload)
            ?.putBoolean("fade", fade)
            ?.putBoolean("offline", offline)
            ?.putBoolean("headphones", headphones)
            ?.putInt("crossfade", crossfade)
            ?.putBoolean("eqOn", eqOn)
            ?.putString("eqLevels", eqLevels)
            ?.putInt("bassBoost", bassBoost)
            ?.putInt("theme", theme)
            ?.putBoolean("lite", lite)
            ?.apply()
    }
}

@Composable
fun statusTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

// ------------------------------------------------------------ full slide-in pages (replace all pop-ups)

object Pages {
    var open by mutableStateOf<String?>(null)
    var song by mutableStateOf<Song?>(null)
    var playlistId by mutableStateOf<String?>(null)
    var nameTitle by mutableStateOf("")
    var nameInitial by mutableStateOf("")
    var collTitle by mutableStateOf("")
    var collUrl by mutableStateOf("")
    var collKind by mutableStateOf(1)
    private var nameOk: ((String) -> Unit)? = null
    private var onClosed: (() -> Unit)? = null

    fun show(key: String, song: Song? = null, playlistId: String? = null) {
        this.song = song
        this.playlistId = playlistId
        onClosed = null
        open = key
    }

    fun askName(title: String, initial: String, onConfirm: (String) -> Unit, onCancel: () -> Unit = {}) {
        nameTitle = title
        nameInitial = initial
        nameOk = onConfirm
        onClosed = onCancel
        open = "name"
    }

    fun openCollection(title: String, url: String, kind: Int) {
        collTitle = title
        collUrl = url
        collKind = kind
        song = null
        onClosed = null
        open = "collection"
    }

    fun confirmName(n: String) {
        val ok = nameOk
        nameOk = null
        onClosed = null
        open = null
        ok?.invoke(n)
    }

    fun close() {
        val cb = onClosed
        onClosed = null
        open = null
        cb?.invoke()
    }
}

@Composable
fun PagesHost(s: AppState) {
    DiscordBridge(s)
    val key = Pages.open
    var last by remember { mutableStateOf("") }
    if (key != null && key != last) last = key
    val vert = (key ?: last) == "lyrics"
    AnimatedVisibility(
        visible = key != null,
        enter = if (vert) {
            androidx.compose.animation.slideInVertically(tween(520, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(420))
        } else {
            slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200))
        },
        exit = if (vert) {
            androidx.compose.animation.slideOutVertically(tween(380, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(tween(300))
        } else {
            slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200))
        }
    ) {
        when (last) {
            "tempo" -> TempoPage(s)
            "queue" -> QueuePage(s)
            "sleep" -> SleepPage(s)
            "add" -> AddPage(s, Pages.song)
            "details" -> DetailsPage(s, Pages.song)
            "songmenu" -> SongMenuPage(s, Pages.song, Pages.playlistId)
            "plmenu" -> PlaylistMenuPage(s, Pages.playlistId)
            "name" -> NamePage(s)
            "stats" -> StatsPage(s)
            "collection" -> CollectionPage(s)
            "lyrics" -> LyricsScreen(s)
            "eq" -> EqPage(s)
            "car" -> CarPage(s)
            else -> Box(Modifier.fillMaxSize().background(Night))
        }
    }
}

@Composable
fun SlidePage(s: AppState, title: String, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    BackHandler { Pages.close() }
    val cur = s.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Night)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        if (cur != null) {
            Cover(cur, if (Opts.lite) Modifier.fillMaxSize() else Modifier.fillMaxSize().blur(60.dp), 0.dp, 120.sp, quality = if (Opts.lite) 200 else 400)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { Pages.close() })
                Spacer(Modifier.width(14.dp))
                Text(title, color = Color.White, fontFamily = PrismFont, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                content = content
            )
        }
    }
}

@Composable
fun PageCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
fun PageRow(icon: ImageVector, label: String, sub: String? = null, tint: Color = Color.White, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .softClick(onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(18.dp))
        Column {
            Text(label, color = Color.White, fontSize = 18.sp)
            if (sub != null) Text(sub, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}

@Composable
fun PageButton(text: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .bounceClick(onClick)
            .clip(RoundedCornerShape(50))
            .background(accent)
            .padding(horizontal = 26.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
}

@Composable
private fun StepIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun SongHeader(song: Song) {
    PageCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(song, Modifier.size(64.dp), 14.dp, 22.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(song.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun fieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = accent,
    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
    cursorColor = accent
)

// ------------------------------------------------------------ tempo and pitch

@Composable
private fun TempoPage(s: AppState) {
    var stMode by remember { mutableStateOf(true) }
    fun semis(): Int = Math.round(12.0 * Math.log(s.pitch.toDouble()) / Math.log(2.0)).toInt()
    fun setTempo(v: Float) {
        s.tempo = Math.round(v.coerceIn(0.25f, 2f) * 20) / 20f
        s.applyTempoPitch()
    }
    fun setSemis(n: Int) {
        s.pitch = Math.pow(2.0, n.coerceIn(-12, 12) / 12.0).toFloat()
        s.applyTempoPitch()
    }
    fun setRatio(v: Float) {
        s.pitch = Math.round(v.coerceIn(0.5f, 2f) * 20) / 20f
        s.applyTempoPitch()
    }
    SlidePage(s, "Tempo and Pitch") {
        PageCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Speed, contentDescription = null, tint = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.width(14.dp))
                Text("Tempo", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Text("x" + "%.2f".format(s.tempo), color = Color.White, fontSize = 20.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepIcon(Icons.Rounded.Remove) { setTempo(s.tempo - 0.05f) }
                SeekBar(
                    positionProvider = { s.tempo - 0.25f },
                    max = 1.75f,
                    onSeek = { setTempo(it + 0.25f) },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                StepIcon(Icons.Rounded.Add) { setTempo(s.tempo + 0.05f) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { v ->
                    Chip("x" + "%.2f".format(v), Math.abs(s.tempo - v) < 0.001f, s.accent) { setTempo(v) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        PageCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.width(14.dp))
                Text("Pitch", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Text(if (stMode) semis().toString() else "%.2f".format(s.pitch), color = Color.White, fontSize = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("st", stMode, s.accent) { stMode = true }
                Chip("x", !stMode, s.accent) { stMode = false }
            }
            Spacer(Modifier.height(12.dp))
            if (stMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepIcon(Icons.Rounded.Remove) { setSemis(semis() - 1) }
                    SeekBar(
                        positionProvider = { (semis() + 12).toFloat() },
                        max = 24f,
                        onSeek = { setSemis(Math.round(it) - 12) },
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    StepIcon(Icons.Rounded.Add) { setSemis(semis() + 1) }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-12, -7, -5, 0, 5, 7, 12).forEach { n ->
                        Chip(if (n > 0) "+" + n else n.toString(), semis() == n, s.accent) { setSemis(n) }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepIcon(Icons.Rounded.Remove) { setRatio(s.pitch - 0.05f) }
                    SeekBar(
                        positionProvider = { s.pitch - 0.5f },
                        max = 1.5f,
                        onSeek = { setRatio(it + 0.5f) },
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    StepIcon(Icons.Rounded.Add) { setRatio(s.pitch + 0.05f) }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { v ->
                        Chip("x" + "%.2f".format(v), Math.abs(s.pitch - v) < 0.001f, s.accent) { setRatio(v) }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PageButton("Reset", s.accent, Modifier.weight(1f)) {
                s.tempo = 1f
                s.pitch = 1f
                s.applyTempoPitch()
            }
            PageButton("Done", s.accent, Modifier.weight(1f)) { Pages.close() }
        }
    }
}

// ------------------------------------------------------------ queue, sleep timer

@Composable
private fun SmallIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun QueuePage(s: AppState) {
    val song = s.current
    var rev by remember { mutableStateOf(0) }
    val list = remember(rev) { s.queue.take(100) }
    fun move(i: Int, d: Int) {
        val l = s.queue.toMutableList()
        val t = i + d
        if (i !in l.indices || t !in l.indices) return
        val x = l.removeAt(i)
        l.add(t, x)
        s.replaceQueue(l)
        rev++
    }
    SlidePage(s, "Queue", scroll = false) {
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(list) { i, q ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { s.play(q, s.queue) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cover(q, Modifier.size(46.dp), 12.dp, 16.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            q.title,
                            color = if (song != null && q.id == song.id) s.accent else Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(q.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1)
                    }
                    SmallIcon(Icons.Rounded.KeyboardArrowUp) { move(i, -1) }
                    SmallIcon(Icons.Rounded.KeyboardArrowDown) { move(i, 1) }
                    SmallIcon(Icons.Rounded.Close) {
                        s.replaceQueue(s.queue.filterIndexed { k, _ -> k != i })
                        rev++
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepPage(s: AppState) {
    SlidePage(s, "Sleep timer") {
        if (s.sleepLeft > 0) {
            Text("Stops in " + fmt(s.sleepLeft), color = s.accent, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
        }
        PageCard {
            listOf(0 to "Off", 5 to "5 minutes", 10 to "10 minutes", 15 to "15 minutes", 30 to "30 minutes", 45 to "45 minutes", 60 to "1 hour", 90 to "1.5 hours").forEach { (m, label) ->
                Text(
                    label,
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            s.sleepLeft = m * 60
                            Pages.close()
                        }
                        .padding(vertical = 14.dp, horizontal = 6.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------ add to playlist, name, song menu

@Composable
private fun AddPage(s: AppState, song: Song?) {
    if (song == null) {
        LaunchedEffect(Unit) { Pages.close() }
        return
    }
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    SlidePage(s, "Add to playlist") {
        SongHeader(song)
        Spacer(Modifier.height(14.dp))
        PageCard {
            Text("New playlist", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text("Playlist name", color = TextDim) },
                colors = fieldColors(s.accent),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            PageButton("Create and add", s.accent) {
                if (name.isNotBlank()) {
                    val id = s.createPlaylist(name.trim())
                    s.addToPlaylist(id, song)
                    Toast.makeText(ctx, "Added", Toast.LENGTH_SHORT).show()
                    Pages.close()
                }
            }
        }
        if (s.playlists.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            PageCard {
                Text("Your playlists", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                s.playlists.forEach { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                s.addToPlaylist(p.id, song)
                                Toast.makeText(ctx, "Added to " + p.name, Toast.LENGTH_SHORT).show()
                                Pages.close()
                            }
                            .padding(vertical = 14.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.name, color = Color.White, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(p.songs.size.toString(), color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NamePage(s: AppState) {
    var name by remember(Pages.nameInitial, Pages.nameTitle) { mutableStateOf(Pages.nameInitial) }
    SlidePage(s, Pages.nameTitle) {
        PageCard {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text("Playlist name", color = TextDim) },
                colors = fieldColors(s.accent),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            PageButton("Save", s.accent) { if (name.isNotBlank()) Pages.confirmName(name.trim()) }
        }
    }
}

@Composable
private fun SongMenuPage(s: AppState, song: Song?, playlistId: String?) {
    if (song == null) {
        LaunchedEffect(Unit) { Pages.close() }
        return
    }
    val ctx = LocalContext.current
    val liked = s.isLiked(song)
    val down = DownloadStore.isDownloaded(song)
    SlidePage(s, "Song options") {
        SongHeader(song)
        Spacer(Modifier.height(14.dp))
        PageCard {
            PageRow(
                if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (liked) "Remove from liked" else "Like",
                tint = if (liked) s.accent else Color.White
            ) {
                s.toggleLike(song)
                Pages.close()
            }
            PageRow(Icons.Rounded.QueueMusic, "Play next") {
                s.playNext(song)
                Toast.makeText(ctx, "Plays next", Toast.LENGTH_SHORT).show()
                Pages.close()
            }
            PageRow(Icons.Rounded.QueueMusic, "Add to end of queue") {
                s.addToQueue(song)
                Toast.makeText(ctx, "Added to queue", Toast.LENGTH_SHORT).show()
                Pages.close()
            }
            PageRow(Icons.Rounded.PlaylistAdd, "Add to playlist") { Pages.show("add", song) }
            PageRow(Icons.Rounded.Download, if (down) "Remove download" else "Download") {
                if (down) DownloadStore.remove(ctx, song) else DownloadStore.start(ctx, song)
                Pages.close()
            }
            PageRow(Icons.Rounded.ContentCopy, "Copy link") {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("link", song.url))
                Toast.makeText(ctx, "Link copied", Toast.LENGTH_SHORT).show()
                Pages.close()
            }
            PageRow(Icons.Rounded.Share, "Share") {
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, song.title + " - " + song.artist + "\n" + song.url)
                }
                ctx.startActivity(Intent.createChooser(i, null))
            }
            if (playlistId != null) {
                PageRow(Icons.Rounded.Delete, "Remove from this playlist") {
                    s.removeFromPlaylist(playlistId, song.id)
                    Pages.close()
                }
            }
        }
    }
}

@Composable
private fun PlaylistMenuPage(s: AppState, id: String?) {
    val pl = s.playlists.find { it.id == id }
    if (pl == null) {
        LaunchedEffect(Unit) { Pages.close() }
        return
    }
    SlidePage(s, pl.name) {
        PageCard {
            PageRow(Icons.Rounded.Edit, "Rename") {
                Pages.askName("Rename playlist", pl.name, { n -> s.renamePlaylist(pl.id, n) })
            }
            PageRow(Icons.Rounded.Delete, "Delete playlist") {
                Pages.close()
                s.openPlaylist = null
                s.deletePlaylist(pl.id)
            }
        }
    }
}

// ------------------------------------------------------------ details, stats

@Composable
private fun DetailsPage(s: AppState, song: Song?) {
    if (song == null) {
        LaunchedEffect(Unit) { Pages.close() }
        return
    }
    SlidePage(s, "Details") {
        SongHeader(song)
        Spacer(Modifier.height(14.dp))
        PageCard {
            Text("Title", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Text(song.title, color = Color.White, fontSize = 17.sp)
            Spacer(Modifier.height(12.dp))
            Text("Artist", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Text(song.artist, color = Color.White, fontSize = 17.sp)
            if (song.seconds > 1) {
                Spacer(Modifier.height(12.dp))
                Text("Length", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                Text(fmt(song.seconds), color = Color.White, fontSize = 17.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Link", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Text(song.url, color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    PageCard(modifier) {
        Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
    }
}

@Composable
private fun StatsPage(s: AppState) {
    val top = remember { s.topPlayed().take(5) }
    val artists = remember { s.topArtists() }
    SlidePage(s, "Your recap") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Songs played", "" + s.playCount, Modifier.weight(1f))
            StatCard("Minutes listened", "" + (s.listenSeconds / 60), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Liked songs", "" + s.liked.size, Modifier.weight(1f))
            StatCard("Playlists", "" + s.playlists.size, Modifier.weight(1f))
        }
        if (top.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("Top songs", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            PageCard {
                top.forEachIndexed { i, sg ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { s.play(sg, top) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text((i + 1).toString(), color = s.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                        Cover(sg, Modifier.size(44.dp), 10.dp, 14.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(sg.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(sg.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        if (artists.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("Top artists", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            PageCard {
                artists.forEachIndexed { i, a ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text((i + 1).toString(), color = s.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                        Text(a.first, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(a.second.toString() + " plays", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsPage(s: AppState) {
    val song = s.current
    var lines by remember(song?.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var busy by remember(song?.id) { mutableStateOf(true) }
    LaunchedEffect(song?.id) {
        busy = true
        lines = if (song != null && !Opts.offline) Lyrics.fetch(song) else emptyList()
        busy = false
    }
    val synced = lines.isNotEmpty() && lines[0].ms >= 0
    val posMs = (s.position * 1000).toLong()
    val active = if (synced) lines.indexOfLast { it.ms <= posMs + 200 } else -1
    val listState = rememberLazyListState()
    LaunchedEffect(active) {
        if (active >= 0) listState.animateScrollToItem((active - 2).coerceAtLeast(0))
    }
    val total = (song?.seconds ?: 1).coerceAtLeast(1).toFloat()
    SlidePage(s, "", scroll = false) {
        if (song != null) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    song.title,
                    color = Color.White,
                    fontFamily = PrismFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(song.artist, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp, maxLines = 1)
            }
        }
        if (busy) {
            LoadingRow(s)
            Spacer(Modifier.weight(1f))
        } else if (lines.isEmpty()) {
            Text(
                "No lyrics found for this song",
                color = TextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 120.dp)
            ) {
                itemsIndexed(lines) { i, ln ->
                    val on = i == active
                    val a by animateFloatAsState(if (!synced || on) 1f else 0.35f, tween(300), label = "lya")
                    val sc by animateFloatAsState(if (on) 1.08f else 0.92f, tween(300), label = "lys")
                    Text(
                        if (ln.text.isEmpty()) "♪" else ln.text,
                        color = (if (on) s.accent else Color.White).copy(alpha = a),
                        fontFamily = PrismFont,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = synced) { s.seekTo(ln.ms / 1000f) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .graphicsLayer {
                                scaleX = sc
                                scaleY = sc
                            }
                    )
                }
            }
        }
        SeekBar({ s.position }, total, { s.seekTo(it) }, Modifier.fillMaxWidth())
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarButton(Icons.Rounded.SkipPrevious, 56.dp, Color.White.copy(alpha = 0.14f), Color.White) { s.prev() }
            PlayButton(s, 72.dp, 36.dp)
            CarButton(Icons.Rounded.SkipNext, 56.dp, Color.White.copy(alpha = 0.14f), Color.White) { s.next() }
        }
    }
}

@Composable
private fun EqPage(s: AppState) {
    val eq = EqHolder.eq
    var on by remember { mutableStateOf(Opts.eqOn) }
    var bass by remember { mutableStateOf(Opts.bassBoost.toFloat()) }
    SlidePage(s, "Equalizer") {
        if (eq == null) {
            Text("Play a song first, then open the equalizer again.", color = TextDim, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            val lo = eq.bandLevelRange[0].toInt()
            val hi = eq.bandLevelRange[1].toInt()
            val n = eq.numberOfBands.toInt()
            val levels = remember {
                mutableStateListOf<Float>().also { l -> for (b in 0 until n) l.add(eq.getBandLevel(b.toShort()).toFloat()) }
            }
            fun ensureOn() {
                if (!on) {
                    on = true
                    Opts.eqOn = true
                    EqHolder.setEnabled(true)
                    Opts.save()
                }
            }
            PageCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Equalizer", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = on,
                        onCheckedChange = {
                            on = it
                            Opts.eqOn = it
                            EqHolder.setEnabled(it)
                            Opts.save()
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Flat", "Bass", "Vocal", "Treble").forEach { name ->
                    Chip(name, false, s.accent) {
                        val v = EqHolder.preset(name, n, lo, hi)
                        for (b in 0 until n) {
                            levels[b] = v[b]
                            EqHolder.setBand(b, v[b].toInt())
                        }
                        ensureOn()
                        EqHolder.saveLevels()
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PageCard {
                for (b in 0 until n) {
                    val hz = eq.getCenterFreq(b.toShort()) / 1000
                    Text(if (hz >= 1000) "%.1f kHz".format(hz / 1000f) else hz.toString() + " Hz", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Slider(
                        value = levels[b].coerceIn(lo.toFloat(), hi.toFloat()),
                        onValueChange = {
                            levels[b] = it
                            EqHolder.setBand(b, it.toInt())
                        },
                        valueRange = lo.toFloat()..hi.toFloat(),
                        enabled = on,
                        onValueChangeFinished = { EqHolder.saveLevels() }
                    )
                }
            }
            if (EqHolder.bass != null) {
                Spacer(Modifier.height(12.dp))
                PageCard {
                    Text("Bass boost", color = Color.White, fontSize = 18.sp)
                    Slider(
                        value = bass,
                        onValueChange = {
                            bass = it
                            EqHolder.setBass(it.toInt())
                        },
                        valueRange = 0f..1000f,
                        onValueChangeFinished = { Opts.save() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CarButton(icon: ImageVector, size: Dp, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(size).bounceClick(onClick).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
private fun CarPage(s: AppState) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val song = s.current
    SlidePage(s, "Car mode", scroll = false) {
        if (song == null) {
            Text("Play a song first", color = TextDim, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Cover(song, Modifier.size(220.dp), 36.dp, 80.sp, quality = 800)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(song.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 20.sp, maxLines = 1)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    CarButton(Icons.Rounded.SkipPrevious, 96.dp, Color.White.copy(alpha = 0.14f), Color.White) { s.prev() }
                    CarButton(if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 128.dp, s.accent, Color.Black) { s.toggle() }
                    CarButton(Icons.Rounded.SkipNext, 96.dp, Color.White.copy(alpha = 0.14f), Color.White) { s.next() }
                }
            }
        }
    }
}

// ------------------------------------------------------------ search: albums, playlists, artists

@Composable
fun HitRow(h: Hit, s: AppState) {
    val fake = Song(h.url, h.title, h.sub, 0, h.url, h.thumb)
    Row(
        Modifier
            .fillMaxWidth()
            .softClick { Pages.openCollection(h.title, h.url, h.kind) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(fake, Modifier.size(60.dp), if (h.kind == 3) 30.dp else 14.dp, 20.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(h.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (if (h.kind == 1) "Album" else if (h.kind == 2) "Playlist" else "Artist") + (if (h.sub.isNotEmpty() && h.kind != 3) "  •  " + h.sub else ""),
                color = TextDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CollectionPage(s: AppState) {
    val url = Pages.collUrl
    val title = Pages.collTitle
    val kind = Pages.collKind
    var songs by remember(url + title) { mutableStateOf<List<Song>>(emptyList()) }
    var busy by remember(url + title) { mutableStateOf(true) }
    var err by remember(url + title) { mutableStateOf("") }
    LaunchedEffect(url + title) {
        busy = true
        err = ""
        try {
            songs = if (kind == 3) Engine.search(title) else Engine.collection(url)
        } catch (e: Exception) {
            err = "Couldn't load: " + (e.message ?: "error").take(60)
        }
        busy = false
    }
    SlidePage(s, title, scroll = false) {
        LazyColumn(Modifier.weight(1f)) {
            if (busy) item { LoadingRow(s) }
            if (err.isNotEmpty()) item { Text(err, color = TextDim, modifier = Modifier.padding(vertical = 12.dp)) }
            if (songs.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PageButton("Play", s.accent, Modifier.weight(1f)) { s.play(songs[0], songs) }
                        PageButton("Shuffle", s.accent, Modifier.weight(1f)) {
                            s.shuffle = true
                            s.play(songs.random(), songs)
                        }
                    }
                }
            }
            items(songs) { SongRow(it, s, songs) }
        }
    }
}


// ------------------------------------------------------------ tempo and pitch: simple middle dialog (ArchiveTune style)

@Composable
fun TempoPitchDialog(s: AppState, onDismiss: () -> Unit) {
    fun semis(): Int = Math.round(12.0 * Math.log(s.pitch.toDouble()) / Math.log(2.0)).toInt()
    fun setTempo(v: Float) {
        s.tempo = Math.round(v.coerceIn(0.25f, 2f) * 20) / 20f
        s.applyTempoPitch()
    }
    fun setSemis(n: Int) {
        s.pitch = Math.pow(2.0, n.coerceIn(-12, 12) / 12.0).toFloat()
        s.applyTempoPitch()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF241F1F),
        shape = RoundedCornerShape(32.dp),
        title = { Text("Tempo and Pitch", color = Color.White, fontSize = 28.sp) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = s.accent, fontSize = 17.sp) } },
        dismissButton = {
            TextButton(onClick = {
                s.tempo = 1f
                s.pitch = 1f
                s.applyTempoPitch()
            }) { Text("Reset", color = s.accent, fontSize = 17.sp) }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speed, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(30.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StepIcon(Icons.Rounded.Remove) { setTempo(s.tempo - 0.05f) }
                    }
                    Text(
                        "x" + "%.2f".format(s.tempo),
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.3f)
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StepIcon(Icons.Rounded.Add) { setTempo(s.tempo + 0.05f) }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(30.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StepIcon(Icons.Rounded.Remove) { setSemis(semis() - 1) }
                    }
                    Text(
                        (if (semis() > 0) "+" else "") + semis(),
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.3f)
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StepIcon(Icons.Rounded.Add) { setSemis(semis() + 1) }
                    }
                }
            }
        }
    )
}


// ------------------------------------------------------------ Home: big "Keep listening" card

@Composable
fun FeaturedCard(song: Song, from: List<Song>, s: AppState, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    val art = rememberArtColor(song)
    val fade = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.40f to Color.Transparent,
            0.75f to Color.Black,
            1f to Color.Black
        )
    }
    Box(
        modifier
            .height(330.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
    ) {
        Cover(song, Modifier.fillMaxSize(), 0.dp, 90.sp, quality = 900)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = fade, blendMode = BlendMode.DstIn)
                }
        ) { Cover(song, Modifier.fillMaxSize().blur(24.dp), 0.dp, 90.sp, quality = 400) }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.40f to Color.Transparent,
                    1f to lerp(Color.Black, art, 0.25f).copy(alpha = 0.92f)
                )
            )
        )
        Row(
            Modifier
                .padding(18.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(s.accent))
            Spacer(Modifier.width(8.dp))
            Text("KEEP LISTENING", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
            Text(
                song.title,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(song.artist, color = Color.White.copy(alpha = 0.75f), fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier
                        .bounceClick { s.play(song, from) }
                        .clip(RoundedCornerShape(50))
                        .background(s.accent)
                        .padding(horizontal = 24.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Play", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
                Row(
                    Modifier
                        .bounceClick {
                            s.shuffle = true
                            s.play(from.random(), from)
                        }
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}


// ------------------------------------------------------------ floating bottom bar

@Composable
fun FloatingNavBar(s: AppState) {
    val shape = RoundedCornerShape(36.dp)
    val tabs = listOf(
        Triple(Screen.Home, Icons.Rounded.Home, "Home"),
        Triple(Screen.Search, Icons.Rounded.Search, "Search"),
        Triple(Screen.Library, Icons.Rounded.LibraryMusic, "Library"),
        Triple(Screen.Settings, Icons.Rounded.Settings, "Settings")
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface1.copy(alpha = 0.97f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (scr, icon, label) ->
            val sel = s.screen == scr
            val a by animateFloatAsState(if (sel) 1f else 0f, tween(260), label = "navsel")
            val accent = s.accent
            Row(
                Modifier
                    .weight(if (sel) 1.7f else 1f)
                    .height(52.dp)
                    .drawBehind {
                        if (a > 0.01f) {
                            if (!Opts.lite) {
                                drawRoundRect(
                                    brush = Brush.radialGradient(
                                        listOf(accent.copy(alpha = 0.30f * a), Color.Transparent),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = size.width * 0.75f
                                    ),
                                    topLeft = Offset(-12.dp.toPx(), -10.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(size.width + 24.dp.toPx(), size.height + 20.dp.toPx()),
                                    cornerRadius = CornerRadius(40.dp.toPx())
                                )
                            }
                            drawRoundRect(accent.copy(alpha = 0.18f * a), cornerRadius = CornerRadius(size.height / 2f))
                        }
                    }
                    .bounceClick {
                        s.openPlaylist = null
                        s.libraryOpen = null
                        s.screen = scr
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = lerp(TextDim, accent, a), modifier = Modifier.size(26.dp))
                if (sel) {
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

// ------------------------------------------------------------ welcome: "made by flagsnerd", word by word

@Composable
fun GlowWords(accent: Color) {
    val words = listOf("made", "by", "flagsnerd")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        words.forEachIndexed { i, w ->
            val a = remember { Animatable(0f) }
            val g = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(900L + i * 330L)
                launch { a.animateTo(1f, tween(420)) }
                g.animateTo(1f, tween(280))
                g.animateTo(if (i == 2) 0.85f else 0.3f, tween(700))
            }
            val c = if (i == 2) accent else Color.White
            Text(
                w,
                color = c,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                style = TextStyle(shadow = Shadow(color = c, offset = Offset.Zero, blurRadius = 4f + 22f * g.value)),
                modifier = Modifier.graphicsLayer {
                    alpha = a.value
                    translationY = (1f - a.value) * 24f
                }
            )
        }
    }
}
