package com.prismwave.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
        setContent {
            val s = remember { AppState() }
            PrismTheme(s.accent) {
                var splash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1300)
                    splash = false
                }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1000)
                        s.tick()
                    }
                }
                Crossfade(targetState = splash, label = "splash") { showSplash ->
                    if (showSplash) Splash() else Main(s)
                }
            }
        }
    }
}

@Composable
fun Splash() {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(700)) }
    Box(Modifier.fillMaxSize().background(Night), contentAlignment = Alignment.Center) {
        Column(Modifier.alpha(a.value), horizontalAlignment = Alignment.CenterHorizontally) {
            PLogo(84.dp, Gold)
            Spacer(Modifier.height(16.dp))
            Text("PrismWave", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Main(s: AppState) {
    Box(Modifier.fillMaxSize().background(Night)) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (s.screen) {
                Screen.Home -> HomeScreen(s)
                Screen.Search -> SearchScreen(s)
                Screen.Library -> LibraryScreen(s)
                Screen.Settings -> SettingsScreen(s)
            }
        }
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
        if (s.showPlayer) FullPlayer(s)
    }
}
