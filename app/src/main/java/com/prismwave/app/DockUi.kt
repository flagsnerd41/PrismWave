package com.prismwave.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// A thin ring of light runs around the edge of the mini player, in the colors of the cover.
@Composable
fun Modifier.glowRing(color: Color, radius: Dp): Modifier {
    val tr = rememberInfiniteTransition(label = "ring")
    val a by tr.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringA"
    )
    val light = lerp(color, Color.White, 0.55f)
    return this.drawWithContent {
        drawContent()
        if (Opts.lite) return@drawWithContent
        val w = size.width
        val h = size.height
        val cr = radius.toPx()
        val bw = 2.dp.toPx()
        val outer = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(cr))) }
        val inner = Path().apply {
            addRoundRect(RoundRect(bw, bw, w - bw, h - bw, CornerRadius((cr - bw).coerceAtLeast(0f))))
        }
        val ring = Path().apply { op(outer, inner, PathOperation.Difference) }
        clipPath(ring) {
            rotate(a, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.16f to light,
                        0.32f to Color.Transparent,
                        0.62f to Color.Transparent,
                        0.80f to color,
                        1f to Color.Transparent,
                        center = center
                    ),
                    radius = maxOf(w, h),
                    center = center
                )
            }
        }
    }
}

private class NavTab(val screen: Screen, val filled: ImageVector, val outlined: ImageVector, val label: String)

// tapping the Search tab bumps this, and the typing box in the bar takes the focus and opens the keyboard
object DockSearch {
    var focusTick by mutableStateOf(0)
}

// One glass bar with three tabs. The chosen tab grows and shows its name. Search turns into a typing box.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavBar2(s: AppState) {
    val tabs = listOf(
        NavTab(Screen.Home, Icons.Rounded.Home, Icons.Outlined.Home, "Home"),
        NavTab(Screen.Search, Icons.Rounded.Search, Icons.Outlined.Search, "Search"),
        NavTab(Screen.Library, Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "Library")
    )
    val searchMode = s.screen == Screen.Search
    val focus = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    val fm = LocalFocusManager.current
    LaunchedEffect(DockSearch.focusTick) {
        if (DockSearch.focusTick > 0) {
            delay(260)
            try {
                focus.requestFocus()
            } catch (e: Exception) {
            }
            kb?.show()
        }
    }
    LaunchedEffect(searchMode) {
        if (!searchMode) {
            fm.clearFocus()
            kb?.hide()
        }
    }
    LiquidGlass(s.glassAlpha(), Modifier.fillMaxWidth().height(68.dp), 34.dp) {
        Row(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEach { t ->
                val sel = s.screen == t.screen
                val isSearch = t.screen == Screen.Search
                val target = if (searchMode) (if (isSearch) 5.2f else 0.8f) else if (sel) 1.75f else 1f
                val w by animateFloatAsState(target, spring(dampingRatio = 0.8f, stiffness = 320f), label = "navw")
                val bubble by animateColorAsState(
                    if (sel) s.accent.copy(alpha = 0.28f) else Color.Transparent, tween(300), label = "navbub"
                )
                val fg by animateColorAsState(
                    if (sel) s.accent else Color.White.copy(alpha = 0.8f), tween(300), label = "navfg2"
                )
                val lift by animateFloatAsState(if (sel) 1.12f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "navlift")
                Row(
                    Modifier
                        .weight(w)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(bubble)
                        .softClick {
                            MoodNav.cat = null
                            MoodNav.playlist = null
                            s.screen = t.screen
                            s.libraryOpen = null
                            s.openPlaylist = null
                            if (isSearch) DockSearch.focusTick++
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (sel) t.filled else t.outlined,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.padding(horizontal = if (isSearch && sel) 8.dp else 0.dp).size(26.dp).graphicsLayer {
                            scaleX = lift
                            scaleY = lift
                        }
                    )
                    if (isSearch && sel) {
                        // the typing box
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
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .clickable { s.searchQuery = "" }
                                    .padding(4.dp)
                            )
                        }
                    } else {
                        AnimatedVisibility(
                            visible = sel,
                            enter = fadeIn(tween(220)) + expandHorizontally(),
                            exit = fadeOut(tween(120)) + shrinkHorizontally()
                        ) {
                            Text(
                                t.label,
                                color = fg,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
