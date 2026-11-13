package com.prismwave.app
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.border
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.input.nestedscroll.nestedScroll

import android.os.Build
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
            remember { Opts.init(appCtx); LiquidGlassOpt.init(appCtx); MiniStyle.init(appCtx); VideoSync.init(appCtx); StudioPrefs.init(appCtx); AppColors.init(appCtx) }
            val s = remember { AppState(appCtx) }
            PrismTheme(s.accent) {
                var splashDone by remember { mutableStateOf(false) }
                val blur = remember { Animatable(BLUR0) }
                val shown = remember { Animatable(0f) }
                val logoIn = remember { Animatable(0f) }
                val titleIn = remember { Animatable(0f) }
                val byOut = remember { Animatable(1f) }
                val fly = remember { Animatable(0f) }
                var logoC by remember { mutableStateOf<LayoutCoordinates?>(null) }
                var titleC by remember { mutableStateOf<LayoutCoordinates?>(null) }
                var logoS by remember { mutableStateOf<Rect?>(null) }
                var titleS by remember { mutableStateOf<Rect?>(null) }
                remember { HeaderPos.hide = true }
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
                    delay(2300)
                    logoS = logoC?.takeIf { it.isAttached }?.boundsInRoot()
                    titleS = titleC?.takeIf { it.isAttached }?.boundsInRoot()
                    val canFly = logoS != null && titleS != null && HeaderPos.logo != null && HeaderPos.title != null
                    launch { byOut.animateTo(0f, tween(300)) }
                    if (canFly) {
                        launch { fly.animateTo(1f, tween(850, easing = FastOutSlowInEasing)) }
                        launch {
                            delay(600)
                            HeaderPos.hide = false
                        }
                    } else {
                        launch { shown.animateTo(0f, tween(450)) }
                        HeaderPos.hide = false
                    }
                    blur.animateTo(0f, tween(1400, easing = FastOutSlowInEasing))
                    HeaderPos.hide = false
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
                                        val f = fly.value
                                        var dx = 0f
                                        var dy = 0f
                                        var sc = 1f
                                        val st = logoS
                                        val tg = HeaderPos.logo
                                        if (f > 0f && st != null && tg != null) {
                                            dx = (tg.center.x - st.center.x) * f
                                            dy = (tg.center.y - st.center.y) * f
                                            sc = 1f + (tg.width / st.width - 1f) * f
                                        }
                                        translationX = dx
                                        translationY = (1f - logoIn.value) * 260f + dy
                                        scaleX = sc
                                        scaleY = sc
                                        alpha = 1f - ((f - 0.72f) / 0.28f).coerceIn(0f, 1f)
                                    }
                                ) { StringLightsFrame(112.dp) { Box(Modifier.onGloballyPositioned { logoC = it }) { AppLogo(112.dp) } } }
                                Spacer(Modifier.height(18.dp))
                                Text(
                                    "PrismWave",
                                    color = Color.White,
                                    fontFamily = PrismFont,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.onGloballyPositioned { titleC = it }.graphicsLayer {
                                        val f = fly.value
                                        var dx = 0f
                                        var dy = 0f
                                        var sc = 1f
                                        val st = titleS
                                        val tg = HeaderPos.title
                                        if (f > 0f && st != null && tg != null) {
                                            sc = 1f + (0.75f - 1f) * f
                                            dx = (tg.left + st.width * 0.75f / 2f - st.center.x) * f
                                            dy = (tg.center.y - st.center.y) * f
                                        }
                                        translationX = dx
                                        translationY = (1f - titleIn.value) * 140f + dy
                                        scaleX = sc
                                        scaleY = sc
                                        alpha = titleIn.value * (1f - ((f - 0.6f) / 0.3f).coerceIn(0f, 1f))
                                    }
                                )
                                Spacer(Modifier.height(10.dp))
                                Box(Modifier.graphicsLayer { alpha = byOut.value }) { GlowLetters(s.accent) }
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
    val blurActive = s.blurOn && !s.batterySaver && !Opts.lite && Build.VERSION.SDK_INT >= 31
    SideEffect {
        backdrop.radius = blurR
        backdrop.enabled = blurActive
    }
    val cur = s.current
    val artC = if (cur != null && Opts.livingBg) rememberArtColor(cur) else s.accent
    val strength = (if (cur != null && Opts.livingBg) 1f else 0.5f) * (if (Opts.theme == 2) 0.35f else 1f)
    val tintTop by animateColorAsState(lerp(Night, artC, 0.42f * strength), tween(900), label = "tintTop")
    val tintBot by animateColorAsState(lerp(Night, artC, 0.16f * strength), tween(900), label = "tintBot")
    LaunchedEffect(artC, AppColors.ver, AppColors.palette) {
        if (Opts.livingBg) {
            if (cur != null) s.accent = lerp(artC, Color.White, 0.35f)
        } else {
            s.accent = AppColors.palette
        }
    }
    var scrolled by remember { mutableStateOf(0f) }
    LaunchedEffect(s.screen, s.openPlaylist, s.libraryOpen) { scrolled = 0f }
    val showBar by remember { derivedStateOf { scrolled > 6f } }
    val scrollConn = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                scrolled = (scrolled - consumed.y).coerceAtLeast(0f)
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
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
                            Box(
                                Modifier.fillMaxSize()
                            ) {
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
                visible = true,
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
                NavCatScene(s)
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
                enter = if (MiniCover.rect != null && !s.reduceMotion) fadeIn(tween(380)) else slideInVertically(tween(520, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(380)),
                exit = if (MiniCover.rect != null && !s.reduceMotion && PlayerStage.stage == 0 && Pages.open != "lyrics") fadeOut(tween(320)) else slideOutVertically(tween(420, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(tween(320))
            ) { FullPlayer(s) }
            CoverFlight(s)
            PagesHost(s)
            VideoWarmup(s)
            ScrobbleWatcher(s)
            if (s.showLogin) LoginScreen(s)
        }
    }
}


@Composable
fun GlowLetters(accent: Color) {
    val text = "made by flagsnerd"
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { i, ch ->
            if (ch == ' ') {
                Spacer(Modifier.width(9.dp))
            } else {
                val p = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(600L + i * 70L)
                    p.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                }
                val glow = kotlin.math.sin(p.value * Math.PI.toFloat())
                Text(
                    ch.toString(),
                    color = lerp(accent, Color.White, p.value),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    style = TextStyle(shadow = Shadow(accent.copy(alpha = 0.9f * glow), Offset.Zero, 18f * glow + 0.1f)),
                    modifier = Modifier.graphicsLayer {
                        alpha = p.value
                        val r = (1f - p.value) * 16f
                        renderEffect = if (Build.VERSION.SDK_INT >= 31 && r > 0.5f) BlurEffect(r, r, TileMode.Decal) else null
                    }
                )
            }
        }
    }
}
