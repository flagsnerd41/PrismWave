package com.prismwave.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ------------------------------------------------------------ building blocks (ArchiveTune style)

@Composable
private fun PfLabel(text: String, accent: Color) {
    Text(
        text,
        color = accent,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 36.dp, end = 20.dp, top = 22.dp, bottom = 6.dp)
    )
}

@Composable
private fun PfGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        verticalArrangement = Arrangement.spacedBy(3.dp),
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
    below: @Composable () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardBg)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                if (sub != null) {
                    Text(sub, color = TextDim, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
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
private fun PfDotSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit
) {
    val span = range.endInclusive - range.start
    val frac = ((value - range.start) / span).coerceIn(0f, 1f)
    val change = rememberUpdatedState(onChange)
    val finish = rememberUpdatedState(onFinish)
    Box(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(range) {
                detectTapGestures { o ->
                    change.value(range.start + (o.x / size.width).coerceIn(0f, 1f) * span)
                    finish.value()
                }
            }
            .pointerInput(range) {
                detectHorizontalDragGestures(
                    onDragEnd = { finish.value() },
                    onDragCancel = { finish.value() }
                ) { c, _ ->
                    c.consume()
                    change.value(range.start + (c.position.x / size.width).coerceIn(0f, 1f) * span)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val h = 22.dp.toPx()
            val top = (size.height - h) / 2f
            val gap = 6.dp.toPx()
            val x = frac * size.width
            val left = (x - gap).coerceAtLeast(0f)
            val rightStart = (x + gap).coerceAtMost(size.width)
            if (left > 0f) {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(0f, top),
                    size = Size(left, h),
                    cornerRadius = CornerRadius(h / 2f)
                )
            }
            if (rightStart < size.width) {
                drawRoundRect(
                    color = accent.copy(alpha = 0.28f),
                    topLeft = Offset(rightStart, top),
                    size = Size(size.width - rightStart, h),
                    cornerRadius = CornerRadius(h / 2f)
                )
            }
            val dotR = 2.dp.toPx()
            val step = 14.dp.toPx()
            var dx = h / 2f
            while (dx < size.width - h / 2f) {
                if (dx < left - 4.dp.toPx()) {
                    drawCircle(Night.copy(alpha = 0.55f), dotR, Offset(dx, size.height / 2f))
                } else if (dx > rightStart + 4.dp.toPx()) {
                    drawCircle(accent, dotR, Offset(dx, size.height / 2f))
                }
                dx += step
            }
            drawRoundRect(
                color = accent,
                topLeft = Offset((x - 2.dp.toPx()).coerceAtLeast(0f), 0f),
                size = Size(4.dp.toPx(), size.height),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
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
        below = {
            Spacer(Modifier.height(14.dp))
            PfDotSlider(value, range, accent, onChange, onFinish)
        }
    )
}

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
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(options.getOrElse(selected) { "" }, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
        }
    )
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = CardBg2,
            title = { Text(title, color = Color.White) },
            text = {
                Column {
                    options.forEachIndexed { i, o ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelect(i)
                                    open = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = i == selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = accent)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(o, color = Color.White, fontSize = 17.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { open = false }) { Text("Close", color = accent) } }
        )
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
        trailing = { Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium) }
    )
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
            shape = RoundedCornerShape(18.dp),
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
                    Gold, Color(0xFF4DB6AC), Color(0xFFE57A8A), Color(0xFF8AB4F8), Color(0xFFB39DDB), Color(0xFFFFA45C)
                ).forEach { c ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(if (c == s.accent) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                            .clickable {
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

// ------------------------------------------------------------ main list

private class PfItem(val key: String, val title: String, val sub: String, val icon: ImageVector)

@Composable
private fun PfMainGroup(items: List<PfItem>, accent: Color, base: Int, onOpen: (String) -> Unit) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text(item.sub, color = TextDim, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ------------------------------------------------------------ the screen

@Composable
fun SettingsScreen(s: AppState) {
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) s.exportTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) s.importFrom(uri) }
    var page by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = page != null) { page = null }
    val acc = s.accent
    val titles = mapOf(
        "account" to "Account",
        "stats" to "Listening Stats",
        "appearance" to "Appearance",
        "player" to "Player and audio",
        "content" to "Content",
        "behavior" to "Behavior",
        "integration" to "Integration",
        "storage" to "Storage",
        "backup" to "Backup and restore",
        "changelog" to "What's new",
        "about" to "About"
    )
    AnimatedContent(
        targetState = page,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val forward = targetState != null
            if (s.reduceMotion) {
                fadeIn(tween(120)) togetherWith fadeOut(tween(80))
            } else {
                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> if (forward) w / 4 else -w / 4 } +
                    fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> if (forward) -w / 4 else w / 4 } +
                        fadeOut(tween(160)))
            }
        },
        label = "settingsPage2"
    ) { pg ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = statusTop(), bottom = 220.dp)
        ) {
            if (pg == null) {
                Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)) {
                    RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { s.screen = Screen.Home })
                    Spacer(Modifier.height(26.dp))
                    Text("Settings", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Medium)
                }
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
                        PfItem("player", "Player and audio", "Crossfade, fades, and headphones", Icons.Rounded.MusicNote),
                        PfItem("content", "Content", "Choose what shows on Home", Icons.Rounded.Home),
                        PfItem("behavior", "Behavior", "Haptics and tap feel", Icons.Rounded.TouchApp)
                    ),
                    acc, 2
                ) { page = it }
                PfMainGroup(
                    listOf(
                        PfItem("integration", "Integration", "Last.fm, Libre.fm, and ListenBrainz", Icons.Rounded.AutoAwesome),
                        PfItem("storage", "Storage", "Cached songs and size limit", Icons.Rounded.Download)
                    ),
                    acc, 6
                ) { page = it }
                PfMainGroup(
                    listOf(
                        PfItem("backup", "Backup and restore", "Save or bring back your library", Icons.Rounded.Folder),
                        PfItem("changelog", "What's new", "Changes in each version", Icons.Rounded.History),
                        PfItem("about", "About", "Version and build info", Icons.Rounded.Info)
                    ),
                    acc, 8
                ) { page = it }
            } else {
                Row(
                    Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { page = null })
                    Spacer(Modifier.width(16.dp))
                    Text(
                        titles[pg] ?: "Settings",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                when (pg) {
                    "account" -> PfAccountPage(s)
                    "stats" -> PfStatsPage(s)
                    "appearance" -> PfAppearancePage(s)
                    "player" -> PfPlayerPage(s)
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

// ------------------------------------------------------------ pages

@Composable
private fun PfAccountPage(s: AppState) {
    val a = s.accent
    PfLabel("YouTube Music", a)
    PfGroup {
        if (s.signedIn) {
            PfAction(
                Icons.Rounded.Sync,
                if (s.importing) "Importing..." else "Import playlists",
                if (s.accountMsg.isNotEmpty()) s.accountMsg else "Bring over playlists and liked songs",
                a,
                !s.importing
            ) { s.importAccount() }
            PfAction(Icons.Rounded.Lock, "Sign out", "You are signed in", a) { s.signOut() }
        } else {
            PfAction(
                Icons.Rounded.AccountCircle,
                "Sign in",
                if (s.accountMsg.isNotEmpty()) s.accountMsg else "Bring over your playlists and liked songs",
                a
            ) { s.showLogin = true }
        }
    }
}

@Composable
private fun PfStatsPage(s: AppState) {
    val a = s.accent
    PfLabel("Your numbers", a)
    PfGroup {
        PfInfo(Icons.Rounded.MusicNote, "Songs played", s.playCount.toString(), a)
        PfInfo(Icons.Rounded.History, "Listening time", (s.listenSeconds / 60).toString() + " min", a)
        PfInfo(Icons.Rounded.AutoAwesome, "Liked songs", s.liked.size.toString(), a)
        PfInfo(Icons.Rounded.Folder, "Playlists", s.playlists.size.toString(), a)
    }
}

@Composable
private fun PfAppearancePage(s: AppState) {
    val a = s.accent
    PfLabel("Theme", a)
    PfGroup {
        PfChoice(
            Icons.Rounded.Palette, "Theme", listOf("Warm", "Dark", "AMOLED", "Midnight"), Opts.theme, a
        ) {
            Opts.theme = it
            Opts.save()
        }
        PfAccent(s)
    }
    PfLabel("Player", a)
    PfGroup {
        PfChoice(
            Icons.Rounded.PlayArrow, "Player design style", listOf("Album", "Immersive"), s.playerStyle, a,
            "More styles are coming soon"
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
private fun PfIntegrationPage(s: AppState) {
    val a = s.accent
    val ctx = LocalContext.current
    Scrobbler.init(ctx.applicationContext)
    val scope = rememberCoroutineScope()
    var lbInput by remember { mutableStateOf("") }
    var lbMsg by remember { mutableStateOf("") }
    var lbBusy by remember { mutableStateOf(false) }
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
    PfNote("A song is scrobbled after half of it has played (or 4 minutes), and only if it is longer than 30 seconds.")
}

@Composable
private fun PfBackupPage(s: AppState, onBackup: () -> Unit, onRestore: () -> Unit) {
    val a = s.accent
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
    PfLabel("PrismWave", a)
    PfGroup {
        PfInfo(Icons.Rounded.Info, "Version", "v24", a)
        PfNote("Private build, made from scratch")
    }
}

@Composable
private fun PfChangelogPage(s: AppState) {
    val a = s.accent
    PfChange("v24", a, listOf(
        "New app background: navy to blue to teal to blue gradient",
        "Liquid glass menu bar (Android 13+), own switch in Appearance",
        "Full player is a sheet with rounded top corners",
        "App icon zoomed a bit, media notification icon is a white P",
        "Song and Video pills in the player (experiment)",
        "Safer song cache and recovery if a song stops too early"
    ))
    PfChange("v23", a, listOf(
        "Settings rebuilt in a new style: grouped cards, switches with a check, dotted sliders, value pickers",
        "New Integration page: scrobble to Last.fm, Libre.fm and ListenBrainz",
        "Settings pages regrouped: Appearance, Player and audio, Content, Behavior, Storage"
    ))
    PfChange("v22.3", a, listOf("New PrismWave logo in the media notification"))
    PfChange("v22.2", a, listOf("New app background: dark navy to blue to teal gradient, no more yellow tint"))
    PfChange("v22.1", a, listOf(
        "Home header shows your YouTube Music profile picture when you are signed in",
        "Lighter glass blur on the bars",
        "Splash: made by flagsnerd appears letter by letter, blurred to clear",
        "New app icon"
    ))
    PfChange("v22", a, listOf(
        "New Home design: greeting header, Featured card, Recently Played and Speed Dial shelves",
        "Lighter glass on the bars: the blur runs on a quarter-size copy",
        "Softer highlight on the glass edge"
    ))
    PfChange("v21", a, listOf(
        "Cache rebuilt: songs are saved as real files and play from there",
        "Lyrics button on the player and a full-screen karaoke view"
    ))
    PfChange("v20", a, listOf(
        "Fix: audio cutting a few seconds into the next song when the app is in the background",
        "The player now moves to the next song by itself, so playback never stops in between"
    ))
    PfChange("v19", a, listOf(
        "Themes: Warm, Dark, AMOLED, Midnight",
        "Floating bottom bar with a glowing selected tab",
        "Title bar that fades in as you scroll",
        "Welcome animation: made by flagsnerd",
        "Gradient cards and bolder titles",
        "Lite mode for low-end phones"
    ))
    PfChange("v18", a, listOf(
        "Synced lyrics (Options, Lyrics)",
        "In-app equalizer with presets and bass boost",
        "Fade between songs, slider in Behavior",
        "Real radio: endless related songs",
        "Queue: move up/down, remove, Play next, Add to end",
        "Real artist page, Car mode, recap with top songs and artists"
    ))
    PfChange("v17 and earlier", a, listOf(
        "Offline mode, preload, fade in and out, headphone smarts",
        "Search filters, cached songs, status bar blur controls",
        "Swipe song rows and the mini player, shimmer loading",
        "Tempo and Pitch, downloads, Apple Music style playlist page",
        "YouTube Music search, streaming, backup and sign-in"
    ))
}
