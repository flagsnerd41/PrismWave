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

/**
 * a font the person uploaded (.ttf / .otf), kept in the app files.
 * SAFE MODE: a broken font can crash the app on every start. The font is checked before it is used, and if the app
 * crashes in the first seconds while a custom font is active, the next start goes back to the original font by itself.
 */
object CustomFont {
    var family by androidx.compose.runtime.mutableStateOf<FontFamily?>(null)
    var name by androidx.compose.runtime.mutableStateOf<String?>(null)

    // set once when safe mode removed a font, so the app can tell the person
    var restoredNote by androidx.compose.runtime.mutableStateOf(false)
    private var launchedAt = 0L
    private var watching = false

    private fun file(ctx: android.content.Context) = java.io.File(ctx.filesDir, "custom_font.ttf")
    private fun prefs(ctx: android.content.Context) = ctx.getSharedPreferences("prism_font", 0)

    // a real font file starts with one of these four signatures
    private fun looksLikeFont(f: java.io.File): Boolean {
        return try {
            if (!f.exists() || f.length() < 1024L) return false
            val b = ByteArray(4)
            f.inputStream().use { if (it.read(b) != 4) return false }
            val s = String(b, Charsets.ISO_8859_1)
            (b[0].toInt() == 0 && b[1].toInt() == 1 && b[2].toInt() == 0 && b[3].toInt() == 0) ||
                s == "OTTO" || s == "true" || s == "ttcf"
        } catch (e: Throwable) {
            false
        }
    }

    // really loads the font once, so a bad file is caught here and not later while drawing text
    private fun usable(f: java.io.File): Boolean {
        return try {
            looksLikeFont(f) && android.graphics.Typeface.createFromFile(f) != null
        } catch (e: Throwable) {
            false
        }
    }

    private fun dropFont(ctx: android.content.Context) {
        try {
            file(ctx).delete()
        } catch (e: Throwable) {
        }
        family = null
        name = null
        prefs(ctx).edit().remove("name").putBoolean("crashed", false).commit()
    }

    /** call first thing in onCreate: sets the crash watcher and loads the font unless safe mode steps in */
    fun load(ctx: android.content.Context) {
        launchedAt = android.os.SystemClock.elapsedRealtime()
        if (!watching) {
            watching = true
            val prev = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { th, e ->
                try {
                    // a crash in the first 20 seconds with a custom font active is blamed on the font
                    if (family != null && android.os.SystemClock.elapsedRealtime() - launchedAt < 20000L) {
                        prefs(ctx).edit().putBoolean("crashed", true).commit()
                    }
                } catch (x: Throwable) {
                }
                if (prev != null) prev.uncaughtException(th, e)
            }
        }
        val f = file(ctx)
        if (!f.exists()) return
        if (prefs(ctx).getBoolean("crashed", false)) {
            dropFont(ctx)
            restoredNote = true
            return
        }
        try {
            if (!usable(f)) {
                dropFont(ctx)
                restoredNote = true
                return
            }
            family = FontFamily(Font(f, FontWeight.Normal), Font(f, FontWeight.Medium), Font(f, FontWeight.SemiBold), Font(f, FontWeight.Bold))
            name = prefs(ctx).getString("name", "Custom font")
        } catch (e: Throwable) {
            dropFont(ctx)
            restoredNote = true
        }
    }

    fun import(ctx: android.content.Context, uri: android.net.Uri, label: String): Boolean {
        val f = file(ctx)
        val tmp = java.io.File(ctx.filesDir, "custom_font_new.tmp")
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            // the new file is checked BEFORE it replaces the old one, so a false font never gets in
            if (!usable(tmp)) {
                tmp.delete()
                false
            } else {
                tmp.copyTo(f, overwrite = true)
                tmp.delete()
                prefs(ctx).edit().putString("name", label).putBoolean("crashed", false).commit()
                family = FontFamily(Font(f, FontWeight.Normal), Font(f, FontWeight.Medium), Font(f, FontWeight.SemiBold), Font(f, FontWeight.Bold))
                name = label
                true
            }
        } catch (e: Throwable) {
            try {
                tmp.delete()
            } catch (x: Throwable) {
            }
            false
        }
    }

    fun reset(ctx: android.content.Context) {
        dropFont(ctx)
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
