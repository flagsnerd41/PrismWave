package com.prismwave.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.ColorScheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val DefaultFont = FontFamily(
    Font(R.font.fraunces, FontWeight.Normal, variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(400), androidx.compose.ui.text.font.FontVariation.Setting("opsz", 28f), androidx.compose.ui.text.font.FontVariation.Setting("SOFT", 40f), androidx.compose.ui.text.font.FontVariation.Setting("WONK", 0f))),
    Font(R.font.fraunces, FontWeight.Medium, variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(500), androidx.compose.ui.text.font.FontVariation.Setting("opsz", 28f), androidx.compose.ui.text.font.FontVariation.Setting("SOFT", 40f), androidx.compose.ui.text.font.FontVariation.Setting("WONK", 0f))),
    Font(R.font.fraunces, FontWeight.SemiBold, variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(600), androidx.compose.ui.text.font.FontVariation.Setting("opsz", 28f), androidx.compose.ui.text.font.FontVariation.Setting("SOFT", 40f), androidx.compose.ui.text.font.FontVariation.Setting("WONK", 0f))),
    Font(R.font.fraunces, FontWeight.Bold, variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(700), androidx.compose.ui.text.font.FontVariation.Setting("opsz", 28f), androidx.compose.ui.text.font.FontVariation.Setting("SOFT", 40f), androidx.compose.ui.text.font.FontVariation.Setting("WONK", 0f)))
)
val AppFont: FontFamily get() = CustomFont.family ?: DefaultFont
val PrismFont: FontFamily get() = AppFont

/** a font the person uploaded (.ttf / .otf), kept in the app files */
object CustomFont {
    var family by androidx.compose.runtime.mutableStateOf<FontFamily?>(null)
    var name by androidx.compose.runtime.mutableStateOf<String?>(null)

    private fun file(ctx: android.content.Context) = java.io.File(ctx.filesDir, "custom_font.ttf")

    fun load(ctx: android.content.Context) {
        val f = file(ctx)
        if (!f.exists()) return
        try {
            android.graphics.Typeface.createFromFile(f)
            family = FontFamily(Font(f, FontWeight.Normal), Font(f, FontWeight.Medium), Font(f, FontWeight.SemiBold), Font(f, FontWeight.Bold))
            name = ctx.getSharedPreferences("prism_font", 0).getString("name", "Custom font")
        } catch (e: Exception) {
            family = null
            f.delete()
        }
    }

    fun import(ctx: android.content.Context, uri: android.net.Uri, label: String): Boolean {
        return try {
            val f = file(ctx)
            ctx.contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { input.copyTo(it) } }
            android.graphics.Typeface.createFromFile(f)
            ctx.getSharedPreferences("prism_font", 0).edit().putString("name", label).apply()
            load(ctx)
            family != null
        } catch (e: Exception) {
            file(ctx).delete()
            family = null
            false
        }
    }

    fun reset(ctx: android.content.Context) {
        file(ctx).delete()
        family = null
        name = null
    }
}

private fun buildTypography(AppFont: FontFamily): androidx.compose.material3.Typography {
    val d = androidx.compose.material3.Typography()
    return d.copy(
        displayLarge = d.displayLarge.copy(fontFamily = AppFont),
        displayMedium = d.displayMedium.copy(fontFamily = AppFont),
        displaySmall = d.displaySmall.copy(fontFamily = AppFont),
        headlineLarge = d.headlineLarge.copy(fontFamily = AppFont),
        headlineMedium = d.headlineMedium.copy(fontFamily = AppFont),
        headlineSmall = d.headlineSmall.copy(fontFamily = AppFont),
        titleLarge = d.titleLarge.copy(fontFamily = AppFont),
        titleMedium = d.titleMedium.copy(fontFamily = AppFont),
        titleSmall = d.titleSmall.copy(fontFamily = AppFont),
        bodyLarge = d.bodyLarge.copy(fontFamily = AppFont),
        bodyMedium = d.bodyMedium.copy(fontFamily = AppFont),
        bodySmall = d.bodySmall.copy(fontFamily = AppFont),
        labelLarge = d.labelLarge.copy(fontFamily = AppFont),
        labelMedium = d.labelMedium.copy(fontFamily = AppFont),
        labelSmall = d.labelSmall.copy(fontFamily = AppFont)
    )
}

/** the generated Material color scheme (like ArchiveTune), null when "Material You" is off */
object ThemeState {
    var scheme by mutableStateOf<ColorScheme?>(null)
    val styleNames = listOf("Tonal spot", "Vibrant", "Expressive", "Rainbow", "Fruit salad", "Monochrome", "Neutral", "Content")
    private val styles = listOf(
        PaletteStyle.TonalSpot, PaletteStyle.Vibrant, PaletteStyle.Expressive, PaletteStyle.Rainbow,
        PaletteStyle.FruitSalad, PaletteStyle.Monochrome, PaletteStyle.Neutral, PaletteStyle.Content
    )
    fun make(seed: Color, style: Int): ColorScheme =
        dynamicColorScheme(seedColor = seed, isDark = true, style = styles[style.coerceIn(0, styles.size - 1)])
}

val CardBg: Color get() = ThemeState.scheme?.surfaceContainerHigh ?: Color.White.copy(alpha = 0.07f)
val CardBg2: Color get() = ThemeState.scheme?.surfaceContainerHighest ?: Color.White.copy(alpha = 0.11f)

val OldGold = Color(0xFFE3C96B)
val PrismBlue = Color(0xFF8FB8FF)
val Gold = PrismBlue
class Palette(val night: Color, val s1: Color, val s2: Color, val dim: Color)

private val palettes = listOf(
    Palette(Color(0xFF0B1220), Color(0xFF141C2E), Color(0xFF1C2740), Color(0xFFA9B4CC)), // Warm (now blue, no yellow)
    Palette(Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFF2A2A2A), Color(0xFFB0B0B0)), // Dark
    Palette(Color(0xFF000000), Color(0xFF0D0D0D), Color(0xFF1A1A1A), Color(0xFFA8A8A8)), // AMOLED
    Palette(Color(0xFF0B1220), Color(0xFF141C2E), Color(0xFF1C2740), Color(0xFFA9B4CC)), // Midnight
    Palette(Color(0xFF121411), Color(0xFF1B1D19), Color(0xFF252822), Color(0xFFC3C8BA))  // ArchiveTune: calm neutral dark, only a light tint of the cover
    ,Palette(Color(0xFF0F0720), Color(0xFF1B1038), Color(0xFF2A1A52), Color(0xFFCDBBF5))  // Vibrant: deep violet, strong cover colors
)

private fun pal(): Palette = palettes[Opts.theme.coerceIn(0, palettes.size - 1)]

val Night: Color get() = ThemeState.scheme?.surface ?: pal().night
val Surface1: Color get() = ThemeState.scheme?.surfaceContainer ?: pal().s1
val Surface2: Color get() = ThemeState.scheme?.surfaceContainerHigh ?: pal().s2
val TextDim: Color get() = ThemeState.scheme?.onSurfaceVariant ?: pal().dim

@Composable
fun PrismTheme(accent: Color = PrismBlue, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ThemeState.scheme ?: darkColorScheme(
            primary = accent,
            onPrimary = Color.Black,
            background = Night,
            surface = Night,
            onSurface = Color.White,
            surfaceVariant = Surface2,
            onSurfaceVariant = TextDim
        ),
        typography = androidx.compose.runtime.remember(CustomFont.family) { buildTypography(AppFont) },
        content = content
    )
}
