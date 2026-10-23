package com.prismwave.app

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent {
            val appCtx = LocalContext.current.applicationContext
            val s = remember { AppState(appCtx) }
            PrismTheme(s.accent) {
                var splashDone by remember { mutableStateOf(false) }
                val blur = remember { Animatable(BLUR0) }
                val shown = remember { Animatable(0f) }
                val logoIn = remember { Animatable(0f) }
                val titleIn = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1000)
                        s.tick()
                    }
                }
                LaunchedEffect(Unit) {
                    launch { shown.animateTo(1f, tween(350)) }
                    launch { logoIn.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 180f)) }
                    delay(350)
                    launch { titleIn.animateTo(1f, tween(600)) }
                    delay(1250)
                    launch { shown.animateTo(0f, tween(450)) }
                    blur.animateTo(0f, tween(1400, easing = FastOutSlowInEasing))
                    splashDone = true
                }
                Box(Modifier.fillMaxSize().background(Night)) {
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            renderEffect = if (Build.VERSION.SDK_INT >= 31 && blur.value > 0.5f) {
                                BlurEffect(blur.value, blur.value, TileMode.Clamp)
                            } else {
                                null
                            }
                        }
                    ) { Main(s) }
                    if (!splashDone) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) { detectTapGestures { } }
                                .background(Color.Black.copy(alpha = 0.45f * (blur.value / BLUR0).coerceIn(0f, 1f))),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                Modifier.graphicsLayer { alpha = shown.value },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier.graphicsLayer {
                                        translationY = (1f - logoIn.value) * 260f
                                    }
                                ) { AppLogo(112.dp) }
                                Spacer(Modifier.height(18.dp))
                                Text(
                                    "PrismWave",
                                    color = Color.White,
                                    fontFamily = PrismFont,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = titleIn.value
                                        translationY = (1f - titleIn.value) * 140f
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val BLUR0 = 70f

@Composable
fun Main(s: AppState) {
    val layer = rememberGraphicsLayer()
    val backdrop = remember { Backdrop(layer) }
    val blurR = s.blurRadius
    val blurActive = s.blurOn && !s.batterySaver && Build.VERSION.SDK_INT >= 31
    SideEffect {
        backdrop.radius = blurR
        backdrop.enabled = blurActive
    }
    val cur = s.current
    val artC = if (cur != null) rememberArtColor(cur) else s.accent
    val strength = if (cur != null) 1f else 0.5f
    val tintTop by animateColorAsState(lerp(Night, artC, 0.42f * strength), tween(900), label = "tintTop")
    val tintBot by animateColorAsState(lerp(Night, artC, 0.16f * strength), tween(900), label = "tintBot")
    CompositionLocalProvider(LocalBackdrop provides backdrop, LocalReduceMotion provides s.reduceMotion) {
        Box(Modifier.fillMaxSize().background(Night)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { backdrop.size = it }
                    .drawWithContent {
                        if (blurActive) {
                            layer.record { this@drawWithContent.drawContent() }
                            drawLayer(layer)
                        } else {
                            drawContent()
                        }
                    }
                    .drawBehind { drawRect(Brush.verticalGradient(listOf(tintTop, tintBot))) }
            ) {
                Box(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = s.screen,
                        transitionSpec = {
                            if (s.reduceMotion) {
                                fadeIn(tween(120)) togetherWith fadeOut(tween(80))
                            } else {
                                val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { w -> w * dir / 3 } +
                                    fadeIn(tween(260))) togetherWith
                                    (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { w -> -w * dir / 3 } +
                                        fadeOut(tween(180)))
                            }
                        },
                        label = "screens"
                    ) { scr ->
                        if (scr == Screen.Home) {
                            HomeScreen(s)
                        } else {
                            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                                when (scr) {
                                    Screen.Search -> SearchScreen(s)
                                    Screen.Library -> LibraryScreen(s)
                                    else -> SettingsScreen(s)
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = s.screen == Screen.Home,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) { TopBlur(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 18.dp) }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniPlayer(s)
                NavBar(s)
            }
            BackHandler(enabled = s.screen != Screen.Home || s.libraryOpen != null || s.openPlaylist != null) {
                when {
                    s.openPlaylist != null -> s.openPlaylist = null
                    s.libraryOpen != null -> s.libraryOpen = null
                    else -> s.screen = Screen.Home
                }
            }
            AnimatedVisibility(
                visible = s.showPlayer,
                enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200))
            ) { FullPlayer(s) }
            if (s.showLogin) LoginScreen(s)
        }
    }
}
