package com.prismwave.app

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Which menu bar the app uses: 0 = Classic (the old pill with the round Search button),
// 1 = Pill (the one tab bar with a typing box), 2 = Apple Music (Home, Library, Settings + round Search).
object NavPrefs {
    var style by mutableStateOf(0)
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        style = ctx.getSharedPreferences("prismwave", 0).getInt("navStyle", 0)
    }

    fun set(ctx: Context, v: Int) {
        style = v
        ctx.getSharedPreferences("prismwave", 0).edit().putInt("navStyle", v).apply()
    }

    // the space between the mini player and the menu bar: tighter on the Apple Music one so the two read as one dock
    fun gap(): Dp = if (style == 2) 4.dp else 8.dp
}

@Composable
fun DockNav(s: AppState) {
    when (NavPrefs.style) {
        1 -> NavBar2(s)
        2 -> {
            if (s.screen == Screen.Search) DockSearchField(s)
            AppleNavBar(s)
        }
        else -> {
            if (s.screen == Screen.Search) DockSearchField(s)
            NavBar(s)
        }
    }
}

private fun goTo(s: AppState, screen: Screen) {
    MoodNav.cat = null
    MoodNav.playlist = null
    s.screen = screen
    s.libraryOpen = null
    s.openPlaylist = null
}

// the typing box for Search when the bar has no box of its own
@Composable
private fun DockSearchField(s: AppState) {
    val focus = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        delay(260)
        try {
            focus.requestFocus()
        } catch (e: Exception) {
        }
        kb?.show()
    }
    LiquidGlass(s.glassAlpha(), Modifier.fillMaxWidth().height(56.dp), 28.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = s.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = s.searchQuery,
                onValueChange = { s.searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                cursorBrush = SolidColor(s.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { kb?.hide() }),
                modifier = Modifier.weight(1f).focusRequester(focus),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (s.searchQuery.isEmpty()) {
                            Text("Search songs, artists...", color = TextDim, fontSize = 17.sp, maxLines = 1)
                        }
                        inner()
                    }
                }
            )
            if (s.searchQuery.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).clickable { s.searchQuery = "" }.padding(4.dp)
                )
            }
        }
    }
}

private class AppleTab(val screen: Screen, val filled: ImageVector, val outlined: ImageVector, val label: String)

// Apple Music look: one glass pill with three tabs (icon over name), the chosen one in a light capsule, and a round Search button next to it
@Composable
fun AppleNavBar(s: AppState) {
    val tabs = listOf(
        AppleTab(Screen.Home, Icons.Rounded.Home, Icons.Outlined.Home, "Home"),
        AppleTab(Screen.Library, Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "Library"),
        AppleTab(Screen.Settings, Icons.Rounded.Settings, Icons.Outlined.Settings, "Settings")
    )
    val searchSel = s.screen == Screen.Search
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidGlass(s.glassAlpha(), Modifier.weight(1f).height(68.dp), 34.dp) {
            Row(Modifier.fillMaxSize().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tabs.forEach { t ->
                    val sel = s.screen == t.screen
                    val bg by animateColorAsState(if (sel) Color.White.copy(alpha = 0.16f) else Color.Transparent, tween(260), label = "apbg")
                    val fg by animateColorAsState(if (sel) s.accent else Color.White.copy(alpha = 0.8f), tween(260), label = "apfg")
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(29.dp))
                            .background(bg)
                            .softClick { goTo(s, t.screen) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(if (sel) t.filled else t.outlined, contentDescription = null, tint = fg, modifier = Modifier.size(25.dp))
                        Text(t.label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        LiquidGlass(s.glassAlpha(), Modifier.size(68.dp), 34.dp) {
            val sbg by animateColorAsState(if (searchSel) s.accent.copy(alpha = 0.24f) else Color.Transparent, tween(250), label = "apsbg")
            Box(
                Modifier.fillMaxSize().background(sbg).softClick { goTo(s, Screen.Search) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = if (searchSel) s.accent else Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

// a small line under the artist: what the stream really is (codec and bitrate)
@Composable
fun StreamLine(song: Song, color: Color) {
    val t = StreamMeta.byUrl[song.url] ?: return
    Text(t, color = color, fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Medium)
}
