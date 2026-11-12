/*
 * ArchiveTune (2026)
 * © Rukamori — https://github.com/rukamori
 *
 * Adapted portions of ArchiveTune's LyricsV2 line renderer and LyricsTextWrapping.
 * Original project: https://github.com/rukamori/ArchiveTune
 * Licensed under the GNU General Public License v3.0 or later.
 * This file contains modifications for PrismWave. See the repository LICENSE.
 */

package com.prismwave.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * A line-synced adaptation of ArchiveTune's LRC bounce renderer.
 *
 * When genuine per-word timestamps are available, [timedWords] enables the
 * word-fill renderer. PrismWave's current LRCLIB response supplies line-level
 * LRC timestamps only, so the normal path deliberately uses LRC bounce rather
 * than inventing word timings.
 */
internal data class ArchiveTimedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ArchiveLyricsLine(
    text: String,
    lit: Boolean,
    animate: Boolean,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    center: Boolean,
    currentPositionMs: Long = 0L,
    timedWords: List<ArchiveTimedWord>? = null,
) {
    val units = remember(text) { text.toLyricsWrappingUnits() }
    val scales = remember(units) { List(units.size) { Animatable(1f) } }
    val floats = remember(units) { List(units.size) { Animatable(0f) } }

    LaunchedEffect(lit, animate, units) {
        if (!lit || !animate || timedWords != null) return@LaunchedEffect
        units.indices.forEach { index ->
            launch {
                delay(index * 40L)
                try {
                    scales[index].animateTo(
                        targetValue = 1f + 0.045f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh,
                        ),
                    )
                    scales[index].animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                } finally {
                    withContext(NonCancellable) { scales[index].snapTo(1f) }
                }
            }
            launch {
                delay(index * 40L)
                try {
                    floats[index].animateTo(
                        targetValue = -5f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh,
                        ),
                    )
                    floats[index].animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                } finally {
                    withContext(NonCancellable) { floats[index].snapTo(0f) }
                }
            }
        }
    }

    val arrangement = if (center) Arrangement.Center else Arrangement.Start
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
    ) {
        if (!timedWords.isNullOrEmpty()) {
            timedWords.forEach { word ->
                if (word.text.isBlank()) {
                    Text(
                        text = word.text,
                        color = Color.Transparent,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                    )
                } else {
                    ArchiveTimedWordText(
                        word = word,
                        positionMs = currentPositionMs,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                    )
                }
            }
        } else {
            units.forEachIndexed { index, unit ->
                Text(
                    text = unit,
                    color = Color.White,
                    fontFamily = PrismFont,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scales[index].value
                        scaleY = scales[index].value
                        translationY = floats[index].value * density
                    },
                )
            }
        }
    }
}

@Composable
private fun ArchiveTimedWordText(
    word: ArchiveTimedWord,
    positionMs: Long,
    fontSize: TextUnit,
    lineHeight: TextUnit,
) {
    val duration = (word.endMs - word.startMs).coerceAtLeast(1L)
    val progress = when {
        positionMs >= word.endMs -> 1f
        positionMs <= word.startMs -> 0f
        else -> ((positionMs - word.startMs).toFloat() / duration).coerceIn(0f, 1f)
    }
    val active = positionMs in word.startMs until word.endMs
    val pulse = sin(progress * PI).toFloat()
    val floatTarget = if (active) -4f * pulse else 0f
    val floatOffset by animateFloatAsState(
        targetValue = floatTarget,
        animationSpec = tween(if (active) 50 else 350),
        label = "archiveWordFloat",
    )
    val glow = if (active) (progress * 2f).coerceAtMost(1f) * 0.45f else 0f
    val scale = 1f + 0.015f * pulse

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.graphicsLayer {
            translationY = floatOffset * density
            scaleX = scale
            scaleY = scale
        },
    ) {
        Text(
            text = word.text,
            color = Color.White.copy(alpha = 0.34f),
            fontFamily = PrismFont,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = FontWeight.SemiBold,
        )
        if (progress > 0f) {
            Text(
                text = word.text,
                color = Color.White,
                fontFamily = PrismFont,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = if (glow > 0f) Shadow(Color.White.copy(alpha = glow), blurRadius = 12f * glow) else null,
                ),
                modifier = Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        if (active && progress < 1f) {
                            val edge = 8.dp.toPx()
                            val centerX = (size.width + edge * 2f) * progress - edge
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startX = centerX - edge,
                                    endX = centerX + edge,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                    },
            )
        }
    }
}

/** Splits on grapheme boundaries, keeping whitespace and CJK characters wrap-safe. */
private fun String.toLyricsWrappingUnits(): List<String> {
    if (isEmpty()) return emptyList()
    val units = mutableListOf<String>()
    val current = StringBuilder()
    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
    iterator.setText(this)

    fun flush() {
        if (current.isNotEmpty()) {
            units += current.toString()
            current.clear()
        }
    }

    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        val grapheme = substring(start, end)
        val codePoint = grapheme.codePointAt(0)
        when {
            grapheme.all(Char::isWhitespace) -> {
                current.append(grapheme)
                flush()
            }
            Character.UnicodeScript.of(codePoint) in setOf(
                Character.UnicodeScript.HAN,
                Character.UnicodeScript.HANGUL,
                Character.UnicodeScript.HIRAGANA,
                Character.UnicodeScript.KATAKANA,
            ) -> {
                flush()
                units += grapheme
            }
            else -> current.append(grapheme)
        }
        start = end
        end = iterator.next()
    }
    flush()
    return units
}
