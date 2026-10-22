package com.prismwave.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val PrismFont = FontFamily(Font(R.font.outfit_bold, FontWeight.Bold))

val Gold = Color(0xFFE3C96B)
val Night = Color(0xFF16120C)
val Surface1 = Color(0xFF241F15)
val Surface2 = Color(0xFF2F2819)
val TextDim = Color(0xFFB8AE98)

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
