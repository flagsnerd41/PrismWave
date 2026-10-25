package com.prismwave.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val PrismFont = FontFamily(Font(R.font.outfit_bold, FontWeight.Bold))

val CardBg = Color.White.copy(alpha = 0.07f)
val CardBg2 = Color.White.copy(alpha = 0.11f)

val Gold = Color(0xFFE3C96B)
class Palette(val night: Color, val s1: Color, val s2: Color, val dim: Color)

private val palettes = listOf(
    Palette(Color(0xFF16120C), Color(0xFF241F15), Color(0xFF2F2819), Color(0xFFB8AE98)), // Warm
    Palette(Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFF2A2A2A), Color(0xFFB0B0B0)), // Dark
    Palette(Color(0xFF000000), Color(0xFF0D0D0D), Color(0xFF1A1A1A), Color(0xFFA8A8A8)), // AMOLED
    Palette(Color(0xFF0B1220), Color(0xFF141C2E), Color(0xFF1C2740), Color(0xFFA9B4CC))  // Midnight
)

private fun pal(): Palette = palettes[Opts.theme.coerceIn(0, palettes.size - 1)]

val Night: Color get() = pal().night
val Surface1: Color get() = pal().s1
val Surface2: Color get() = pal().s2
val TextDim: Color get() = pal().dim

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
