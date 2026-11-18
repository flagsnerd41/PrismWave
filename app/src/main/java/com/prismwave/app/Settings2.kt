package com.prismwave.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// M3-SETTINGS ------------------------------------------------------------ building blocks (Material 3)

@Composable
private fun PfLabel(text: String, accent: Color) {
    Text(
        text,
        color = accent,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 32.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

private object PfSeq {
    private var n = 0
    fun next(): Int {
        n = (n + 1) % 7
        return n
    }
}

@Composable
private fun PfGroup(content: @Composable ColumnScope.() -> Unit) {
    val seq = remember { PfSeq.next() }
    Column(
        Modifier
            .appear(seq)
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun PfRow(
    icon: ImageVector?,
    title: String,
    sub: String?,
    accent: Color,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit = {},
    below: @Composable () -> Unit = {},
    tint: Boolean = false
) {
    val rowBg by androidx.compose.animation.animateColorAsState(
        if (tint) androidx.compose.ui.graphics.lerp(CardBg, accent, 0.30f) else CardBg,
        tween(300),
        label = "pfrowbg"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(rowBg)
            .then(if (onClick != null) Modifier.softClick { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                if (sub != null) {
                    Text(sub, color = TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            trailing()
        }
        below()
    }
}

@Composable
private fun PfSwitch(
    icon: ImageVector?,
    title: String,
    sub: String?,
    checked: Boolean,
    accent: Color,
    onChange: (Boolean) -> Unit
) {
    PfRow(
        icon, title, sub, accent, { onChange(!checked) },
        tint = checked,
        trailing = {
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                thumbContent = {
                    Icon(
                        if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Night,
                    checkedTrackColor = accent,
                    checkedIconColor = accent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.5f),
                    uncheckedIconColor = Night
                )
            )
        }
    )
}

@Composable
private fun PfSlider(
    icon: ImageVector?,
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit = {}
) {
    PfRow(
        icon, title, valueText, accent, null,
        below = { PfDotSlider(value, range, accent, onChange, onFinish) }
    )
}

// a fat dotted track with a tall bar as the thumb
@Composable
private fun PfDotSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit
) {
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    val frac = ((value - range.start) / span).coerceIn(0f, 1f)
    var dragging by remember { mutableStateOf(false) }
    val change by rememberUpdatedState(onChange)
    val finish by rememberUpdatedState(onFinish)
    val thumbW by androidx.compose.animation.core.animateDpAsState(if (dragging) 3.dp else 5.dp, tween(150), label = "dsThumb")
    Canvas(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .height(44.dp)
            .pointerInput(range) {
                detectTapGestures { o ->
                    change(range.start + (o.x / size.width).coerceIn(0f, 1f) * span)
                    finish()
                }
            }
            .pointerInput(range) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        finish()
                    },
                    onDragCancel = {
                        dragging = false
                        finish()
                    }
                ) { c, _ ->
                    c.consume()
                    change(range.start + (c.position.x / size.width).coerceIn(0f, 1f) * span)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val trackH = 16.dp.toPx()
        val top = (h - trackH) / 2f
        val gap = 7.dp.toPx()
        val tx = (w * frac).coerceIn(trackH / 2f, w - trackH / 2f)
        val round = CornerRadius(trackH / 2f)
        val leftW = (tx - gap).coerceAtLeast(0f)
        if (leftW > 0.5f) drawRoundRect(accent, Offset(0f, top), Size(leftW, trackH), round)
        val rx = tx + gap
        if (w - rx > 0.5f) drawRoundRect(accent.copy(alpha = 0.26f), Offset(rx, top), Size(w - rx, trackH), round)
        val dot = 1.6f.dp.toPx()
        val step = 10.dp.toPx()
        var x = 9.dp.toPx()
        while (x < leftW - 6.dp.toPx()) {
            drawCircle(Night.copy(alpha = 0.45f), dot, Offset(x, h / 2f))
            x += step
        }
        x = rx + 9.dp.toPx()
        while (x < w - 9.dp.toPx()) {
            drawCircle(accent.copy(alpha = 0.85f), dot, Offset(x, h / 2f))
            x += step
        }
        val tw = thumbW.toPx()
        drawRoundRect(accent, Offset(tx - tw / 2f, h / 2f - 18.dp.toPx()), Size(tw, 36.dp.toPx()), CornerRadius(tw / 2f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PfChoice(
    icon: ImageVector?,
    title: String,
    options: List<String>,
    selected: Int,
    accent: Color,
    sub: String? = null,
    onSelect: (Int) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    PfRow(
        icon, title, sub, accent, { open = true },
        trailing = { Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
        below = {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.16f))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(options.getOrElse(selected) { "" }, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    )
    if (open) {
        // a bottom sheet: big title, one pill per option, the chosen one filled with the accent color
        val sheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val sheetScope = rememberCoroutineScope()
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheet,
            containerColor = Night,
            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
                options.forEachIndexed { i, o ->
                    val chosen = i == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (chosen) accent else CardBg2)
                            .softClick {
                                onSelect(i)
                                sheetScope.launch {
                                    sheet.hide()
                                    open = false
                                }
                            }
                            .padding(horizontal = 28.dp, vertical = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(o, color = if (chosen) Night else Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        if (chosen) Icon(Icons.Rounded.Check, contentDescription = null, tint = Night)
                    }
                }
            }
        }
    }
}

@Composable
private fun PfAction(
    icon: ImageVector?,
    title: String,
    sub: String?,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    PfRow(
        icon, title, sub, if (enabled) accent else TextDim,
        if (enabled) onClick else null
    )
}

@Composable
private fun PfInfo(icon: ImageVector?, title: String, value: String, accent: Color) {
    PfRow(
        icon, title, null, accent, null,
        trailing = { Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
    )
}

@Composable
private fun PfIntro(text: String) {
    Text(text, color = TextDim, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp))
}

@Composable
private fun PfNote(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) { Text(text, color = TextDim, fontSize = 14.sp) }
}

@Composable
private fun PfField(label: String, value: String, accent: Color, password: Boolean, onChange: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            label = { Text(label, color = TextDim) },
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = accent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                cursorColor = accent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PfAccent(s: AppState) {
    PfRow(
        Icons.Rounded.AutoAwesome, "Accent color", "Buttons and highlights", s.accent, null,
        below = {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Gold, Color(0xFF4DB6AC), Color(0xFFE57A8A), Color(0xFF8AB4F8), Color(0xFFB39DDB), Color(0xFFFFA45C), Color(0xFF81C784), Color(0xFFEF5350)
                ).forEach { c ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(if (c == s.accent) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                            .clickable {
                                AppColors.pick(c)
                                s.accent = c
                                s.saveSettings()
                            }
                    )
                }
            }
        }
    )
}

@Composable
private fun PfChange(version: String, accent: Color, lines: List<String>) {
    PfLabel(version, accent)
    PfGroup {
        Column(
            Modifier
                .fillMaxWidth()
                .background(CardBg)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            lines.forEach { Text("•  " + it, color = Color.White, fontSize = 15.sp) }
        }
    }
}

// ------------------------------------------------------------ main list (Material 3)

private class PfItem(val key: String, val title: String, val sub: String, val icon: ImageVector)

@Composable
private fun PfMainGroup(items: List<PfItem>, accent: Color, base: Int, onOpen: (String) -> Unit) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { i, item ->
            val big = 28.dp
            val small = 6.dp
            val top = if (i == 0) big else small
            val bottom = if (i == items.lastIndex) big else small
            Row(
                Modifier
                    .appear(base + i)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                    .background(CardBg)
                    .softClick { onOpen(item.key) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text(item.sub, color = TextDim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PfAboutBanner(accent: Color, onOpen: () -> Unit) {
    Row(
        Modifier
            .appear(0)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(accent.copy(alpha = 0.32f), CardBg)))
            .softClick { onOpen() }
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLogo(56.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("About PrismWave", color = Color.White, fontFamily = RetroBlendy, fontSize = 24.sp)
            Text("Version 1.5 and updates", color = TextDim, fontSize = 14.sp)
        }
        Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}

// ------------------------------------------------------------ the screen (Material 3 large top bar)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(s: AppState) {
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) s.exportTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) s.importFrom(uri) }
    var page by remember { mutableStateOf<String?>(SettingsNav.take()) }
    BackHandler(enabled = page != null) { page = null }
    val acc = s.accent
    val titles = mapOf(
        "account" to "Account",
        "stats" to "Listening Stats",
        "appearance" to "Appearance",
        "player" to "Player and audio",
        "transitions" to "Transitions",
        "content" to "Content",
        "behavior" to "Behavior",
        "integration" to "Integration",
        "storage" to "Storage",
        "backup" to "Backup and restore",
        "changelog" to "What's new",
        "about" to "About PrismWave"
    )
    AnimatedContent(
        targetState = page,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val forward = targetState != null
            if (s.reduceMotion) {
                fadeIn(tween(120)) togetherWith fadeOut(tween(80))
            } else {
                (slideInHorizontally(tween(480, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))) { w -> if (forward) w / 3 else -w / 5 } +
                    fadeIn(tween(340, delayMillis = 60)) +
                    androidx.compose.animation.scaleIn(tween(480, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)), initialScale = 0.94f)) togetherWith
                    (slideOutHorizontally(tween(360, easing = FastOutSlowInEasing)) { w -> if (forward) -w / 6 else w / 3 } +
                        fadeOut(tween(200)) +
                        androidx.compose.animation.scaleOut(tween(360, easing = FastOutSlowInEasing), targetScale = 0.96f))
            }
        },
        label = "settingsPage2"
    ) { pg ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = statusTop())
        ) {
            // one small fixed header like ArchiveTune: back arrow and title in a row, it does not move with the scrolling
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { if (pg == null) s.screen = Screen.Home else page = null },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    if (pg == null) "Settings" else (titles[pg] ?: "Settings"),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 220.dp)
            ) {
                if (pg == null) {
                    PfProfileHeader(s) { page = "account" }
                    PfAboutBanner(acc) { page = "about" }
                    PfMainGroup(
                        listOf(
                            PfItem("account", "Account", "Sign in and manage account sync", Icons.Rounded.AccountCircle),
                            PfItem("stats", "Listening Stats", "Your songs played and listening time", Icons.Rounded.BarChart)
                        ),
                        acc, 0
                    ) { page = it }
                    PfMainGroup(
                        listOf(
                            PfItem("appearance", "Appearance", "Themes, blur, and player styles", Icons.Rounded.Palette),
                            PfItem("player", "Player and audio", "Fades, headphones and video", Icons.Rounded.MusicNote),
                            PfItem("transitions", "Transitions", "Crossfade, blur-out, 8D and cover flip", Icons.Rounded.Sync),
                            PfItem("content", "Content", "Choose what shows on Home", Icons.Rounded.Home),
                            PfItem("behavior", "Behavior", "Haptics and tap feel", Icons.Rounded.TouchApp)
                        ),
                        acc, 2
                    ) { page = it }
                    PfMainGroup(
                        listOf(
                            PfItem("integration", "Integration", "Last.fm, Libre.fm, ListenBrainz and Discord", Icons.Rounded.AutoAwesome),
                            PfItem("storage", "Storage", "Cached songs and size limit", Icons.Rounded.Download)
                        ),
                        acc, 7
                    ) { page = it }
                    PfMainGroup(
                        listOf(
                            PfItem("backup", "Backup and restore", "Save or bring back your library", Icons.Rounded.Folder),
                            PfItem("changelog", "What's new", "Changes in each version", Icons.Rounded.History)
                        ),
                        acc, 9
                    ) { page = it }
                } else {
                    when (pg) {
                        "account" -> PfAccountPage(s) { page = it }
                        "stats" -> PfStatsPage(s)
                        "appearance" -> PfAppearancePage(s)
                        "player" -> PfPlayerPage(s)
                        "transitions" -> PfTransitionsPage(s)
                        "content" -> PfContentPage(s)
                        "behavior" -> PfBehaviorPage(s)
                        "integration" -> PfIntegrationPage(s)
                        "storage" -> PfStoragePage(s)
                        "backup" -> PfBackupPage(
                            s,
                            { exportLauncher.launch("PrismWave-backup.json") },
                            { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                        )
                        "changelog" -> PfChangelogPage(s)
                        "about" -> PfAboutPage(s)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ pages

@Composable
private fun PfAccountPage(s: AppState, onOpen: (String) -> Unit) {
    val a = s.accent
    val ctx = LocalContext.current
    LaunchedEffect(s.cookie) {
        if (s.signedIn && ProfilePic.url == null) ProfilePic.url = YtAccount.profilePhoto(s.cookie)
    }
    if (s.signedIn) {
        val url = ProfilePic.url
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(CardBg2)
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp)) {
                    Box(
                        Modifier.fillMaxSize().clip(CircleShape).background(Color.White.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (url != null) {
                            val req = remember(url) { coil.request.ImageRequest.Builder(ctx).data(artUrl(url, 300)).crossfade(300).build() }
                            coil.compose.AsyncImage(
                                model = req, contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            AppLogo(48.dp)
                        }
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(28.dp).clip(CircleShape).background(a),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp)) }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(ProfilePic.name ?: "Your account", color = Color.White, fontFamily = PrismFont, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("YouTube Music", color = a, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(a.copy(alpha = 0.25f))
                        .softClick { s.importAccount() }.padding(horizontal = 20.dp, vertical = 12.dp)
                ) { Text(if (s.importing) "Syncing..." else "Sync now", color = a, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(Modifier.weight(1f))
                Text(
                    "Log out", color = Color(0xFFFF8A8A), fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).softClick { s.signOut() }.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            if (s.accountMsg.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(s.accountMsg, color = TextDim, fontSize = 13.sp)
            }
        }
        PfLabel("General", a)
        PfGroup {
            PfSwitch(
                Icons.Rounded.Sync, "Auto sync with account",
                "Brings over new playlists and liked songs every 6 hours", AccountPrefs.autoSync, a
            ) { AccountPrefs.set(ctx, it) }
            PfAction(
                Icons.Rounded.Sync, if (s.importing) "Importing..." else "Import playlists now",
                "Bring over playlists and liked songs", a, !s.importing
            ) { s.importAccount() }
        }
        PfLabel("Integration", a)
        PfGroup {
            PfAction(Icons.Rounded.AutoAwesome, "Integration", "Discord, Last.fm, Libre.fm, ListenBrainz", a) { onOpen("integration") }
        }
    } else {
        PfIntro("Sign in to YouTube Music to bring over your playlists and liked songs.")
        PfLabel("YouTube Music", a)
        PfGroup {
            PfAction(
                Icons.Rounded.AccountCircle, "Sign in",
                if (s.accountMsg.isNotEmpty()) s.accountMsg else "Bring over your playlists and liked songs", a
            ) { s.showLogin = true }
        }
        PfLabel("What signing in gives you", a)
        PfGroup {
            PfInfo(Icons.Rounded.Folder, "Your playlists", "Brought over", a)
            PfInfo(Icons.Rounded.AutoAwesome, "Liked songs", "Brought over", a)
            PfInfo(Icons.Rounded.Sync, "Home picks", "Made for you", a)
            PfInfo(Icons.Rounded.Lock, "Your password", "Never stored", a)
        }
    }
}

@Composable
private fun PfStatsPage(s: AppState) {
    val a = s.accent
    val ctxS = LocalContext.current
    val mins = (s.listenSeconds / 60).toInt()
    PfIntro("Everything here is counted on this phone only. Back up from Backup and restore to keep it.")
    PfLabel("Your numbers", a)
    PfGroup {
        PfInfo(Icons.Rounded.MusicNote, "Songs played", s.playCount.toString(), a)
        PfInfo(Icons.Rounded.History, "Listening time", (if (mins >= 60) (mins / 60).toString() + " h " + (mins % 60).toString() + " min" else mins.toString() + " min"), a)
        PfInfo(Icons.Rounded.Speed, "Average per song", (if (s.playCount > 0) (s.listenSeconds / s.playCount / 60).toString() + " min" else "-"), a)
        PfInfo(Icons.Rounded.Download, "Cached songs size", SongCache.usedMb(ctxS, 0).toString() + " MB", a)
        PfInfo(Icons.Rounded.AutoAwesome, "Liked songs", s.liked.size.toString(), a)
        PfInfo(Icons.Rounded.Folder, "Playlists", s.playlists.size.toString(), a)
    }
}

@Composable
private fun PfAppearancePage(s: AppState) {
    val a = s.accent
    val fctx = LocalContext.current
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val label = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Custom font"
            val ok = CustomFont.import(fctx, uri, label)
            android.widget.Toast.makeText(fctx, if (ok) "Font added" else "That file is not a usable font", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    PfLabel("Font", a)
    PfGroup {
        PfAction(
            Icons.Rounded.Palette, "Upload a font",
            if (CustomFont.family != null) "Using: " + (CustomFont.name ?: "custom font") else "Pick a .ttf or .otf file",
            a
        ) { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream", "*/*")) }
        if (CustomFont.family != null) {
            PfAction(Icons.Rounded.Sync, "Back to the default font", null, a) { CustomFont.reset(fctx) }
        }
    }
    PfLabel("Theme", a)
    PfGroup {
        PfChoice(
            Icons.Rounded.Palette, "Theme", listOf("Warm", "Dark", "AMOLED", "Midnight", "ArchiveTune"), Opts.theme, a
        ) {
            Opts.theme = it
            Opts.save()
        }
        PfChoice(
            Icons.Rounded.AutoAwesome, "App colors", listOf("Color palette", "Follow music theme"), if (Opts.livingBg) 1 else 0, a,
            "Palette: the colors you pick. Follow music: the app takes the colors of the playing cover"
        ) {
            AppColors.setFollow(it == 1)
            if (it == 0) s.accent = AppColors.palette
            s.saveSettings()
        }
        PfAccent(s)
    }
    val navCtx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    PfLabel("Menu bar", a)
    PfGroup {
        PfChoice(
            Icons.Rounded.Palette, "Menu bar style", listOf("Classic", "Pill", "Apple Music"), NavPrefs.style, a,
            "Classic: the old bar with the round Search button. Pill: one bar with a typing box. Apple Music: icon over name, light capsule on the chosen tab"
        ) {
            NavPrefs.set(navCtx, it)
        }
    }
    PfLabel("Progress bar", a)
    PfGroup {
        PfChoice(
            Icons.Rounded.Speed, "Progress bar style", listOf("Wave", "Material 3", "Line", "Pill", "Dotted"), SeekPrefs.style, a,
            "Wave moves while music plays. Material 3 is the standard slider. Line, Pill and Dotted are PrismWave's own"
        ) {
            SeekPrefs.set(navCtx, it)
        }
    }
    PfLabel("Player", a)
    PfGroup {
        PfChoice(
            Icons.Rounded.PlayArrow, "Player design style", listOf("Album", "Immersive", "Vinyl", "Aurora", "Poster"), s.playerStyle, a,
            "Vinyl: a spinning record with lyrics underneath"
        ) {
            s.playerStyle = it
            s.saveSettings()
        }
    }
    PfLabel("Blur", a)
    PfGroup {
        PfSwitch(Icons.Rounded.AutoAwesome, "Blur on bars", "Glass look on the mini player and menu bar", s.blurOn, a) {
            s.blurOn = it
            s.saveSettings()
        }
        PfSlider(
            Icons.Rounded.Speed, "Blur strength", s.blurRadius.toInt().toString() + " dp (Android 12+)",
            s.blurRadius, 10f..200f, a, { s.blurRadius = it }, { s.saveSettings() }
        )
        PfSlider(
            Icons.Rounded.Palette, "Bar tint", (s.glass * 100).toInt().toString() + "%",
            s.glass, 0.3f..1f, a, { s.glass = it }, { s.saveSettings() }
        )
    }
    PfLabel("Experimental", a)
    PfGroup {
        val lctx = LocalContext.current
        val ok = android.os.Build.VERSION.SDK_INT >= 33
        PfSwitch(
            Icons.Rounded.AutoAwesome, "Liquid glass menu bar",
            if (ok) "Real glass refraction on the bottom bar. Off or Lite mode uses the normal blur" else "Needs Android 13 or newer",
            LiquidGlassOpt.on && ok, a
        ) { if (ok) LiquidGlassOpt.set(lctx, it) }
        PfSwitch(
            Icons.Rounded.Palette, "Gradient mini player",
            "Cover color gradient. Off uses the blur glass", MiniStyle.gradient, a
        ) { MiniStyle.set(lctx, it) }
    }
    PfLabel("Status bar", a)
    PfGroup {
        PfSwitch(Icons.Rounded.AutoAwesome, "Blur under status bar", "Blurred strip on every screen", Opts.statusBlur, a) {
            Opts.statusBlur = it
            Opts.save()
        }
        PfSlider(
            Icons.Rounded.Speed, "Status bar blur", (Opts.blurAmount * 100).toInt().toString() + "%",
            Opts.blurAmount, 0.2f..2f, a, { Opts.blurAmount = it }, { Opts.save() }
        )
        PfSlider(
            Icons.Rounded.Palette, "Status bar darkness", (Opts.blurDark * 100).toInt().toString() + "%",
            Opts.blurDark, 0f..0.9f, a, { Opts.blurDark = it }, { Opts.save() }
        )
    }
    PfLabel("Extras", a)
    PfGroup {
        PfSwitch(Icons.Rounded.AutoAwesome, "Cat on the mini player", "A smart cat with a tree and a food bowl", LookPrefs.catOn, a) {
            LookPrefs.catOn = it
            LookPrefs.save()
        }
        PfSwitch(Icons.Rounded.Palette, "Sky glow on Home", "Sun, sunset or moon that follows the time of day", LookPrefs.skyOn, a) {
            LookPrefs.skyOn = it
            LookPrefs.save()
        }
        PfChoice(
            Icons.Rounded.Palette, "Try the sky", listOf("Auto", "Morning", "Afternoon", "Evening", "Night"),
            when (LookPrefs.previewHour) {
                null -> 0
                8f -> 1
                13f -> 2
                18.5f -> 3
                else -> 4
            },
            a, "Preview the glow for each time of day"
        ) { LookPrefs.previewHour = listOf<Float?>(null, 8f, 13f, 18.5f, 23f)[it] }
        PfSwitch(Icons.Rounded.AutoAwesome, "String lights", "Glowing lights around the logo on the welcome screen", StudioPrefs.lightsOn, a) {
            StudioPrefs.lightsOn = it
            StudioPrefs.save()
        }
    }
    PfLabel("Performance", a)
    PfGroup {
        PfSwitch(Icons.Rounded.Speed, "Reduce motion", "Fewer and shorter animations", s.reduceMotion, a) {
            s.reduceMotion = it
            s.saveSettings()
        }
        PfSwitch(Icons.Rounded.Speed, "Lite mode", "Turns off glow, blur and heavy effects for low-end phones", Opts.lite, a) {
            Opts.lite = it
            Opts.save()
        }
        PfSwitch(Icons.Rounded.Speed, "Battery saver", "No blur at all", s.batterySaver, a) {
            s.batterySaver = it
            s.saveSettings()
        }
    }
}

@Composable
private fun PfPlayerPage(s: AppState) {
    val a = s.accent
    PfLabel("Player", a)
    PfGroup {
        PfSwitch(Icons.Rounded.Download, "Preload next songs", "Loads the next 2 songs while one plays (uses data)", Opts.preload, a) {
            Opts.preload = it
            Opts.save()
        }
        PfSwitch(Icons.Rounded.MusicNote, "Offline mode", "Only show downloaded and cached songs", Opts.offline, a) {
            Opts.offline = it
            Opts.save()
        }
        PfSwitch(Icons.Rounded.Speed, "Headphone smarts", "Pause on disconnect, resume when they reconnect", Opts.headphones, a) {
            Opts.headphones = it
            Opts.save()
        }
    }
    PfLabel("Fades", a)
    PfGroup {
        PfSwitch(Icons.Rounded.PlayArrow, "Fade in and out", "Soft volume fade on play, pause and skip", Opts.fade, a) {
            Opts.fade = it
            Opts.save()
        }
        PfSlider(
            Icons.Rounded.Sync, "Fade between songs",
            if (Opts.crossfade == 0) "Off" else Opts.crossfade.toString() + " s",
            Opts.crossfade.toFloat(), 0f..12f, a,
            { Opts.crossfade = it.toInt() }, { Opts.save() }
        )
        PfSwitch(Icons.Rounded.Speed, "Muffle between songs", "The end gets muffled and quiet, the next song fades in clear", StudioPrefs.mixOn, a) {
            StudioPrefs.mixOn = it
            StudioPrefs.save()
        }
        PfSlider(
            Icons.Rounded.Sync, "Muffle length", StudioPrefs.mixSecs.toString() + " s",
            StudioPrefs.mixSecs.toFloat(), 5f..20f, a,
            { StudioPrefs.mixSecs = it.toInt() }, { StudioPrefs.save() }
        )
    }
    Aod.init(LocalContext.current.applicationContext)
    VolPrefs.init(LocalContext.current.applicationContext)
    PfLabel("Volume", a)
    PfGroup {
        PfSwitch(
            Icons.Rounded.VolumeUp, "Volume slider in the player",
            "Show the volume slider under the controls", VolPrefs.show, a
        ) {
            VolPrefs.show = it
            VolPrefs.save()
        }
        PfSwitch(
            Icons.Rounded.VolumeUp, "Volume in the menu",
            "Show the volume card in the three dots menu", VolPrefs.showMenu, a
        ) {
            VolPrefs.showMenu = it
            VolPrefs.save()
        }
    }
    PfLabel("Idle screen", a)
    PfGroup {
        PfSwitch(
            Icons.Rounded.Speed, "Always on display",
            "Idle on the player: half view, then a black screen with lyrics. The screen stays on while music plays on the player",
            Aod.enabled, a
        ) {
            Aod.enabled = it
            Aod.save()
        }
        PfSlider(
            Icons.Rounded.History, "Half view after", Aod.halfMin.toString() + " min",
            Aod.halfMin.toFloat(), 1f..5f, a,
            { Aod.halfMin = it.toInt().coerceIn(1, 5) }, { Aod.save() }
        )
        PfSlider(
            Icons.Rounded.History, "Black screen after another", Aod.blackMin.toString() + " min",
            Aod.blackMin.toFloat(), 1f..5f, a,
            { Aod.blackMin = it.toInt().coerceIn(1, 5) }, { Aod.save() }
        )
    }
    PfLabel("Video (experiment)", a)
    PfGroup {
        val vctx = LocalContext.current
        PfChoice(
            Icons.Rounded.PlayArrow, "Video quality", VideoSync.qualityNames, VideoSync.quality, a,
            "Higher quality uses more data"
        ) { VideoSync.setQuality(vctx, it) }
    }
    PfLabel("Timer", a)
    PfGroup {
        val sl = s.sleepLeft
        PfInfo(
            Icons.Rounded.History, "Sleep timer",
            if (sl > 0) fmt(sl) + " left" else "Off", a
        )
        PfNote("Set the sleep timer from the player.")
    }
}

@Composable
private fun PfContentPage(s: AppState) {
    val a = s.accent
    PfIntro("Pick what shows on Home. Turned off sections are hidden, nothing is deleted.")
    PfLabel("Home sections", a)
    PfGroup {
        PfSwitch(Icons.Rounded.History, "Recently played", "Your recent songs", Opts.showKeep, a) {
            Opts.showKeep = it
            Opts.save()
        }
        PfSwitch(Icons.Rounded.Speed, "Speed dial", "Your most played", Opts.showSpeed, a) {
            Opts.showSpeed = it
            Opts.save()
        }
        PfSwitch(Icons.Rounded.AutoAwesome, "Forgotten favorites", "Liked songs you haven't played lately", Opts.showForgotten, a) {
            Opts.showForgotten = it
            Opts.save()
        }
        PfSwitch(Icons.Rounded.MusicNote, "Chill and Workout", "Mix shelves at the bottom", Opts.showMixes, a) {
            Opts.showMixes = it
            Opts.save()
        }
    }
}

@Composable
private fun PfBehaviorPage(s: AppState) {
    val a = s.accent
    PfIntro("How PrismWave feels and how much power it uses.")
    PfLabel("Power", a)
    PfGroup {
        PfSwitch(Icons.Rounded.Speed, "Battery saver", "Solid bars instead of blur, lighter visuals", s.batterySaver, a) {
            s.batterySaver = it
            s.saveSettings()
        }
        PfSwitch(Icons.Rounded.Sync, "Reduce motion", "Fewer animations everywhere", s.reduceMotion, a) {
            s.reduceMotion = it
            s.saveSettings()
        }
        PfSwitch(Icons.Rounded.Speed, "Lite mode", "For low-end phones: no heavy effects", Opts.lite, a) {
            Opts.lite = it
            Opts.save()
        }
    }
    PfLabel("Feel", a)
    PfGroup {
        PfSwitch(Icons.Rounded.TouchApp, "Haptic feedback", "A small tick when you tap", Opts.haptics, a) {
            Opts.haptics = it
            Opts.save()
        }
        PfSwitch(Icons.Rounded.TouchApp, "Bounce on tap", "Buttons squish when pressed", Opts.bounce, a) {
            Opts.bounce = it
            Opts.save()
        }
    }
}

@Composable
private fun PfStoragePage(s: AppState) {
    val a = s.accent
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    val sizes = listOf(256, 512, 1024, 2048)
    PfIntro("Songs you play are kept here so they replay instantly and work offline.")
    PfLabel("Cached songs", a)
    PfGroup {
        PfInfo(Icons.Rounded.Download, "Using", SongCache.usedMb(ctx, tick).toString() + " MB of " + Opts.cacheMb + " MB", a)
        PfChoice(
            Icons.Rounded.Folder, "Size limit",
            sizes.map { if (it >= 1024) (it / 1024).toString() + " GB" else it.toString() + " MB" },
            sizes.indexOf(Opts.cacheMb).coerceAtLeast(0), a,
            "Oldest songs are removed first when the limit is reached"
        ) {
            Opts.cacheMb = sizes[it]
            Opts.save()
        }
        PfAction(Icons.Rounded.Close, "Clear cache", "Songs you play are saved here for instant and offline playback", a) {
            SongCache.clear(ctx)
            tick++
        }
    }
}

@Composable
private fun PfFmSection(f: FmService, accent: Color, hint: String) {
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf(f.key) }
    var secret by remember { mutableStateOf(f.secret) }
    var user by remember { mutableStateOf(f.user) }
    var pass by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    PfLabel(f.name, accent)
    PfGroup {
        PfSwitch(
            Icons.Rounded.MusicNote, "Scrobble to " + f.name,
            if (f.connected) "Connected as " + f.user else "Not connected",
            f.enabled && f.connected, accent
        ) {
            f.enabled = it && f.connected
            Scrobbler.save()
        }
        if (!f.connected) {
            PfNote(hint)
            PfField("API key", key, accent, false) { key = it }
            PfField("API secret", secret, accent, false) { secret = it }
            PfField("Username", user, accent, false) { user = it }
            PfField("Password", pass, accent, true) { pass = it }
            PfAction(Icons.Rounded.Sync, if (busy) "Connecting..." else "Connect", null, accent, !busy) {
                busy = true
                f.key = key.trim()
                f.secret = secret.trim()
                scope.launch {
                    msg = Scrobbler.fmLogin(f, user.trim(), pass)
                    pass = ""
                    busy = false
                }
            }
        } else {
            PfAction(Icons.Rounded.Lock, "Disconnect", "Stop scrobbling and forget this login", accent) {
                Scrobbler.fmLogout(f)
                msg = ""
            }
        }
        if (msg.isNotEmpty()) PfNote(msg)
    }
}

@Composable
private fun PfDiscordSection() {
    val a = Color(0xFF5865F2)
    val ctx = LocalContext.current
    val logo = ImageVector.vectorResource(R.drawable.ic_discord)
    Row(
        Modifier
            .appear(0)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(a, Color(0xFF4752C4))))
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(logo, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Discord", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Show what you listen to on your profile", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        }
    }
    PfGroup {
        PfSwitch(
            logo, "Discord status",
            if (DiscordRpc.hasToken()) "Shows the song you listen to on your Discord profile"
            else "Sign in to Discord first. Unofficial, so an alt account is safer",
            DiscordRpc.enabled && DiscordRpc.hasToken(), a
        ) { on ->
            if (on && !DiscordRpc.hasToken()) DiscordRpc.loginOpen = true
            else DiscordRpc.setEnabled(ctx, on)
        }
        if (DiscordRpc.hasToken()) {
            PfField("Discord application ID (optional, adds cover art)", DiscordRpc.appId, a, false) {
                DiscordRpc.setAppId(ctx, it)
            }
            PfAction(Icons.Rounded.Lock, "Sign out of Discord", "Forget the Discord login on this phone", a) {
                DiscordRpc.logout(ctx)
            }
        }
        if (!DiscordRpc.hasToken()) {
            PfAction(
                Icons.Rounded.Lock, "Use token from clipboard",
                "If the sign-in page does not work, copy your Discord token and tap here", a
            ) {
                val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val tok = (cm.primaryClip?.getItemAt(0)?.text?.toString() ?: "").trim().trim('"')
                if (tok.length > 40) DiscordRpc.onToken(ctx.applicationContext, tok)
            }
        }
        if (DiscordRpc.status.isNotEmpty()) PfNote(DiscordRpc.status)
    }
    if (DiscordRpc.loginOpen) DiscordLogin()
}

@Composable
private fun PfIntegrationPage(s: AppState) {
    val a = s.accent
    val ctx = LocalContext.current
    Scrobbler.init(ctx.applicationContext)
    DiscordRpc.init(ctx.applicationContext)
    val scope = rememberCoroutineScope()
    var lbInput by remember { mutableStateOf("") }
    var lbMsg by remember { mutableStateOf("") }
    var lbBusy by remember { mutableStateOf(false) }
    PfDiscordSection()
    PfLabel("ListenBrainz", a)
    PfGroup {
        val lbOk = Scrobbler.lbUser.isNotEmpty()
        PfSwitch(
            Icons.Rounded.MusicNote, "Scrobble to ListenBrainz",
            if (lbOk) "Connected as " + Scrobbler.lbUser else "Not connected",
            Scrobbler.lbEnabled && lbOk, a
        ) {
            Scrobbler.lbEnabled = it && lbOk
            Scrobbler.save()
        }
        if (!lbOk) {
            PfNote("Copy your user token from listenbrainz.org/settings and paste it here.")
            PfField("User token", lbInput, a, true) { lbInput = it }
            PfAction(Icons.Rounded.Sync, if (lbBusy) "Connecting..." else "Connect", null, a, !lbBusy) {
                lbBusy = true
                scope.launch {
                    lbMsg = Scrobbler.lbConnect(lbInput)
                    lbInput = ""
                    lbBusy = false
                }
            }
        } else {
            PfAction(Icons.Rounded.Lock, "Disconnect", "Stop scrobbling and forget the token", a) {
                Scrobbler.lbDisconnect()
                lbMsg = ""
            }
        }
        if (lbMsg.isNotEmpty()) PfNote(lbMsg)
    }
    PfFmSection(
        Scrobbler.lastfm, a,
        "Create a free API account at last.fm/api to get a key and secret, then sign in with your username and password."
    )
    PfFmSection(
        Scrobbler.librefm, a,
        "Use your Libre.fm username and password. Enter an API key and secret the same way as for Last.fm."
    )
    PfIntro("A song is scrobbled after half of it has played (or 4 minutes), and only if it is longer than 30 seconds.")
}

@Composable
private fun PfBackupPage(s: AppState, onBackup: () -> Unit, onRestore: () -> Unit) {
    val a = s.accent
    PfIntro("Backups are plain files you choose where to save. Your login is never in them.")
    PfLabel("Backup", a)
    PfGroup {
        PfAction(
            Icons.Rounded.Download, "Back up",
            "Saves liked songs, playlists, history and stats to a file you choose. Your login is never included.", a
        ) { onBackup() }
        PfAction(Icons.Rounded.Folder, "Restore", "Bring back a backup file", a) { onRestore() }
        if (s.backupMsg.isNotEmpty()) PfNote(s.backupMsg)
    }
}

@Composable
private fun PfAboutPage(s: AppState) {
    val a = s.accent
    val uctx = LocalContext.current
    val uscope = rememberCoroutineScope()
    val build = (Updater.currentCode(uctx) - 100).coerceAtLeast(0)
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.about_cat),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0.35f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.85f)
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogo(80.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                "PrismWave",
                color = Color.White,
                fontFamily = RetroBlendy,
                fontSize = 40.sp,
                letterSpacing = 0.6.sp
            )
            Text("made by flagsnerd", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
        }
    }
    PfLabel("Version", a)
    PfGroup {
        PfInfo(Icons.Rounded.Info, "Current version", "1.5", a)
        PfAction(
            Icons.Rounded.Sync, "Check for updates",
            if (Updater.status.isNotEmpty()) Updater.status else "Look for a newer version", a,
            Updater.status != "Checking..."
        ) { uscope.launch { Updater.checkNow(uctx) } }
        PfNote("Private build, made from scratch. Build " + build)
    }
}

@Composable
private fun PfChangelogPage(s: AppState) {
    val a = s.accent
    PfChange("Fixed zoomed cover and messy open animation in the player", a, listOf(
        "The Immersive player cover is nearly square now, so it is no longer blown up and cropped",
        "Opening the player: the cover flies to its place alone, then the player fades in around it"
    ))
    PfChange("Added option to hide the volume slider", a, listOf(
        "Settings, Player, Volume: turn the slider off in the player and in the three dots menu"
    ))
    PfChange("Added fresh picks, real Mood & Genres and a typing box in the bar", a, listOf(
        "Home: Fresh for you. New songs every day, built from what you play and like, and songs you already know or saw lately never come back",
        "More shelves: Because you played, More from your top artist, Discover a new style, and a mood for the time of day. Refresh picks gives a new set",
        "Mood & Genres from YouTube Music in Search, with colors and covers. Open a category to see its playlists and play them",
        "Tap Search in the bottom bar and it grows into the typing box, with the keyboard ready"
    ))
    PfChange("Added new dock, tonal settings and bottom sheet pickers", a, listOf(
        "Menu bar: one glass bar with Home, Search and Library. The chosen tab grows, gets a bubble and shows its name",
        "Mini player: a moving glow inside, a ring of light running around the edge, bigger cover and a round play button",
        "Settings: a switch that is on fills its whole row with the accent color",
        "Settings: dotted sliders with a tall thumb, and choices open as a bottom sheet with big pills"
    ))
    PfChange("Added moving glow, Discord on top and smoother settings", a, listOf(
        "Moving glow behind the player controls and the lyrics: bright and dark colors from the cover flow and move. Settings, Appearance",
        "About PrismWave is now the first thing in Settings",
        "Discord is at the top of Integration with its logo",
        "Settings: pages slide and scale in smoothly, cards rise one after another, rows squish when pressed"
    ))
    PfChange("Added idle screen", a, listOf(
        "Idle on the full player: after a minute it goes to the half view, after another minute to a black screen",
        "The black screen has a small cover, the controls, and the lyrics at the bottom with the app icon",
        "Touch the empty black screen to come back. Timers are in Settings, Player",
        "The full player to half player step is slower and smoother when the idle timer does it"
    ))
    PfChange("Added About page, pinned header and new playlist design", a, listOf(
        "About PrismWave in Settings: cat picture, logo, Retro Blendy name, version 1.5 and Check for updates",
        "The Home, Search and Library header stays on top while you scroll, with a fading blur under it",
        "Playlist page like ArchiveTune: full cover on top, big title, round buttons with a Play pill"
    ))
    PfChange("Added ArchiveTune lyrics look", a, listOf(
        "Gradient background from the cover colors, the playing line sits in the middle",
        "The whole line lights up at once, only when it is sung, no word by word",
        "Lines fade and blur the further they are from the playing one"
    ))
    PfChange("Added stage lyrics, Library redesign, vinyl layout, longer muffle", a, listOf(
        "Lyrics no longer open as another page: the cover moves up and shrinks into the corner on the same screen",
        "Half view: the bottom of the cover melts into a blur that continues behind the lyrics",
        "Library redesigned: sliding tabs, big colorful tiles, playlist grid with cover collages, artist grid",
        "Vinyl: a huge record pushed to the right like the reference, the lines of the song at the bottom, swipe to change song",
        "The muffled ending now builds up over the whole length (up to 20 seconds) and the length setting works"
    ))
    PfChange("Added swipe steps and half lyrics view", a, listOf(
        "Swipe up on the player: half view with the cover on top, a blurred bottom and the lyrics underneath",
        "Swipe up again: the full lyrics page. Swipe down goes back one step",
        "Swipe up on the three dots menu opens the half view too",
        "Swipe down on the lyrics header closes the lyrics"
    ))
    PfChange("Added popup menu, fixed cover animation, bigger vinyl cover", a, listOf(
        "The three dots menu is now a popup that grows out of the corner, with parts that rise and sharpen one after another",
        "Fixed the cover animation: the page no longer slides and fades over the flying cover",
        "Vinyl: bigger record and a much bigger cover in the middle"
    ))
    PfChange("Added cover morph and record spin", a, listOf(
        "Opening lyrics: the big cover shrinks into the top corner and the title slides up, like Apple Music",
        "Pause button in the lyrics header",
        "The vinyl record speeds up when you play and slowly winds down when you pause"
    ))
    PfChange("Added Vinyl player and cover stack", a, listOf(
        "New Vinyl player style: a spinning record with the cover as the label, a swirling background and the lyric lines underneath",
        "Cover stack: a 3D stack of your queue, history or likes. Drag to flip through it",
        "Choose Vinyl in Settings, Appearance, Player design style"
    ))
    PfChange("Added lyrics upgrade, fixed video stutter, added UI extras", a, listOf(
        "Lyrics: the playing line fills word by word, pause dots, moving background, sync fix, text size and alignment",
        "Lyrics: better song matching, and lyrics are saved on the phone",
        "Fixed video stutter: no more blur and mask on the video every frame",
        "The status bar hides in the full player",
        "What's new button in the Home header",
        "Playlist covers show a 2 by 2 collage",
        "Search: recent searches, live suggestions and chips that stay at the top"
    ))
    PfChange("Added Home layout, cover preload and removed start fade", a, listOf(
        "Home: big Quick picks carousel, mood chips and From the community, like ArchiveTune",
        "Covers load 30 seconds before the song ends, so the cover shuffle is instant",
        "Songs start at full volume, no fade-in at the start"
    ))
    PfChange("Added AutoMix smart mix point", a, listOf(
        "AutoMix listens to the end of a cached song and starts the transition where the outro begins",
        "Uses your crossfade or blur-out style, with a smart length for every song"
    ))
    PfChange("Added cover blur-out, cover flip and Material 3 settings", a, listOf(
        "Cover blur-out: the album cover slowly blurs as the song ends",
        "Cover flip: the cover square flips over to the next song",
        "Settings rebuilt in Material 3: large collapsing title, tonal icons, Material sliders"
    ))
    PfChange("Added 8D audio and Transitions", a, listOf(
        "8D audio: the sound slowly circles around your head, speed slider",
        "New Transitions page: crossfade, audio blur-out and 8D in one place"
    ))
    PfChange("Added YouTube Music recommendations", a, listOf(
        "Home recommendations come from your own YouTube Music Home: Quick picks, Listen again and more",
        "Not signed in: Picked for you, based on the songs you play most"
    ))
    PfChange("Added animated lyrics and Discord sign-in improvements", a, listOf(
        "Lyrics: current line grows, others shrink, blur and fade, smooth glide",
        "Lyrics: finds a synced version when the first one is plain text",
        "Discord sign-in page opens as a desktop page, with a clipboard backup",
        "Less lag when opening the player: one fetch per song, video check waits",
        "No video button for songs, only for real videos"
    ))
    PfChange("Added synchronized video, blurred lyrics and Discord status", a, listOf(
        "Video and its blurred backdrop start and pause together",
        "Discord status: Integration, Discord",
        "New lyrics page: blurred lines, cover-tinted background, big bold text",
        "Opening lyrics: the player sinks down and the lyrics rise up",
        "Faster playback: song links are remembered, quicker start, quicker preload"
    ))
    PfChange("Added moving video backgrounds and liquid glass player", a, listOf(
        "Video: blurred backdrop now moves with the video, high quality by default",
        "Video quality choice in Player and audio",
        "Song/Video pills slide",
        "Player is full screen with rounded top corners",
        "Liquid glass on the mini player",
        "Media notification icon is now your P logo"
    ))
    PfChange("Added gradient background and liquid glass menu", a, listOf(
        "New app background: navy to blue to teal to blue gradient",
        "Liquid glass menu bar (Android 13+), own switch in Appearance",
        "Full player is a sheet with rounded top corners",
        "App icon zoomed a bit, media notification icon is a white P",
        "Song and Video pills in the player (experiment)",
        "Safer song cache and recovery if a song stops too early"
    ))
    PfChange("Redesigned Settings and added music integrations", a, listOf(
        "Settings rebuilt in a new style: grouped cards, switches with a check, dotted sliders, value pickers",
        "New Integration page: scrobble to Last.fm, Libre.fm and ListenBrainz",
        "Settings pages regrouped: Appearance, Player and audio, Content, Behavior, Storage"
    ))
    PfChange("Updated PrismWave notification logo", a, listOf("New PrismWave logo in the media notification"))
    PfChange("Added the new blue gradient background", a, listOf("New app background: dark navy to blue to teal gradient, no more yellow tint"))
    PfChange("Added profile header and new app icon", a, listOf(
        "Home header shows your YouTube Music profile picture when you are signed in",
        "Lighter glass blur on the bars",
        "Splash: made by flagsnerd appears letter by letter, blurred to clear",
        "New app icon"
    ))
    PfChange("Redesigned Home and improved glass effects", a, listOf(
        "New Home design: greeting header, Featured card, Recently Played and Speed Dial shelves",
        "Lighter glass on the bars: the blur runs on a quarter-size copy",
        "Softer highlight on the glass edge"
    ))
    PfChange("Added offline song cache and karaoke lyrics", a, listOf(
        "Cache rebuilt: songs are saved as real files and play from there",
        "Lyrics button on the player and a full-screen karaoke view"
    ))
    PfChange("Fixed background playback and song transitions", a, listOf(
        "Fix: audio cutting a few seconds into the next song when the app is in the background",
        "The player now moves to the next song by itself, so playback never stops in between"
    ))
    PfChange("Added themes and redesigned navigation", a, listOf(
        "Themes: Warm, Dark, AMOLED, Midnight",
        "Floating bottom bar with a glowing selected tab",
        "Title bar that fades in as you scroll",
        "Welcome animation: made by flagsnerd",
        "Gradient cards and bolder titles",
        "Lite mode for low-end phones"
    ))
    PfChange("Added synced lyrics, equalizer and crossfade", a, listOf(
        "Synced lyrics (Options, Lyrics)",
        "In-app equalizer with presets and bass boost",
        "Fade between songs, slider in Behavior",
        "Real radio: endless related songs",
        "Queue: move up/down, remove, Play next, Add to end",
        "Real artist page, Car mode, recap with top songs and artists"
    ))
    PfChange("Added offline mode, downloads and player features", a, listOf(
        "Offline mode, preload, fade in and out, headphone smarts",
        "Search filters, cached songs, status bar blur controls",
        "Swipe song rows and the mini player, shimmer loading",
        "Tempo and Pitch, downloads, Apple Music style playlist page",
        "YouTube Music search, streaming, backup and sign-in"
    ))
}

@Composable
private fun PfTransitionsPage(s: AppState) {
    val a = s.accent
    val tctx = LocalContext.current.applicationContext
    EightD.init(tctx)
    AutoMix.init(tctx)
    VideoPrefs.init(tctx)
    TransitionPrefs.init(tctx)
    PfLabel("Between songs", a)
    PfGroup {
        PfSwitch(
            Icons.Rounded.AutoAwesome, "AutoMix: smart mix point",
            "Finds where each song's outro starts and begins the transition there. Works on cached songs",
            AutoMix.on, a
        ) { AutoMix.turn(it) }
        PfSwitch(Icons.Rounded.PlayArrow, "Fade in and out", "Soft volume fade on play, pause and skip", Opts.fade, a) {
            Opts.fade = it
            Opts.save()
        }
        PfSlider(
            Icons.Rounded.Sync, "Crossfade",
            if (Opts.crossfade == 0) "Off" else Opts.crossfade.toString() + " s",
            Opts.crossfade.toFloat(), 0f..12f, a,
            { Opts.crossfade = it.toInt() }, { Opts.save() }
        )
        PfSwitch(Icons.Rounded.Speed, "Audio blur-out", "The end gets muffled and quiet, the next song comes in clear", StudioPrefs.mixOn, a) {
            StudioPrefs.mixOn = it
            StudioPrefs.save()
        }
        PfSlider(
            Icons.Rounded.Sync, "Blur-out length", StudioPrefs.mixSecs.toString() + " s",
            StudioPrefs.mixSecs.toFloat(), 5f..20f, a,
            { StudioPrefs.mixSecs = it.toInt() }, { StudioPrefs.save() }
        )
        PfSlider(
            Icons.Rounded.Speed, "Blur-out strength", (StudioPrefs.mixStrength * 100).toInt().toString() + "%",
            StudioPrefs.mixStrength, 0.3f..1f, a,
            { StudioPrefs.mixStrength = it }, { StudioPrefs.save() }
        )
    }
    PfLabel("Album cover", a)
    PfGroup {
        PfSwitch(Icons.Rounded.AutoAwesome, "Cover blur-out", "The cover slowly blurs as the song ends, together with the audio", TransitionPrefs.blur, a) {
            TransitionPrefs.blur = it
            TransitionPrefs.save()
        }
        PfSlider(
            Icons.Rounded.Sync, "Cover blur starts", "Last " + TransitionPrefs.blurSecs.toString() + " s of the song",
            TransitionPrefs.blurSecs.toFloat(), 3f..12f, a,
            { TransitionPrefs.blurSecs = it.toInt() }, { TransitionPrefs.save() }
        )
        PfSwitch(
            Icons.Rounded.Palette, "Random covers",
            "At the blurriest moment the cover shuffles through your songs, then settles on the next one and slowly clears",
            TransitionPrefs.shuffle, a
        ) {
            TransitionPrefs.shuffle = it
            TransitionPrefs.save()
        }
        PfNote("The cover blur uses the same length as the audio blur-out or AutoMix when those are on. Lite mode and Reduce motion turn cover effects off.")
    }
    PfLabel("8D audio", a)
    PfGroup {
        PfSwitch(Icons.Rounded.AutoAwesome, "8D audio", "The sound circles around your head. Best with headphones", EightD.on, a) {
            EightD.turn(it)
        }
        PfSlider(
            Icons.Rounded.Speed, "Circle speed",
            "One turn every " + (1f / EightD.speed).toInt().toString() + " s",
            EightD.speed, 0.04f..0.4f, a,
            { EightD.changeSpeed(it) }, { EightD.save() }
        )
        PfNote("8D works on stereo songs. Higher speed means faster circles.")
    }
    PfLabel("Video", a)
    PfGroup {
        PfSwitch(
            Icons.Rounded.PlayArrow, "Live video backdrop",
            "Blurred video behind the player. Looks great, but can make video stutter on some phones",
            VideoPrefs.liveBackdrop, a
        ) { VideoPrefs.set(it) }
    }
}

// ------------------------------------------------------------ settings header: the account picture

@Composable
private fun PfProfileHeader(s: AppState, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(s.cookie) {
        if (s.signedIn && ProfilePic.url == null) ProfilePic.url = YtAccount.profilePhoto(s.cookie)
    }
    val url = ProfilePic.url
    val accent = s.accent
    Column(
        Modifier
            .appear(0)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(accent.copy(alpha = 0.38f), CardBg)))
            .softClick { onOpen() }
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .border(3.dp, accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (s.signedIn && url != null) {
                val req = remember(url) { coil.request.ImageRequest.Builder(ctx).data(artUrl(url, 400)).crossfade(300).build() }
                coil.compose.AsyncImage(
                    model = req,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                AppLogo(64.dp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (s.signedIn) (ProfilePic.name ?: "Your account") else "PrismWave",
            color = Color.White,
            fontFamily = PrismFont,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(if (s.signedIn) "Tap to manage your account" else "Tap to sign in", color = TextDim, fontSize = 14.sp)
    }
}
