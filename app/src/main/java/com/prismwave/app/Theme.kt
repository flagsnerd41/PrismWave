package com.prismwave.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold = Color(0xFFE8B04B)
val Night = Color(0xFF121110)
val Surface1 = Color(0xFF1C1A18)
val Surface2 = Color(0xFF26231F)
val TextDim = Color(0xFFB5ADA3)

@Composable
fun PrismTheme(accent: Color = Gold, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = Color.Black,
            background = Night,
            surface = Night,
            onSurface = Color.White,
            surfaceVariant = Surface2,
            onSurfaceVariant = TextDim
        ),
        content = content
    )
}
