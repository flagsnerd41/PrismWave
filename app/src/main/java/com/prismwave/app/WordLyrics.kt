package com.prismwave.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

// One real word of a song with the time it starts and ends (from the lyric file, nothing is guessed)
class LyricWord(val startMs: Long, val endMs: Long, val text: String)

// Reads the word-by-word lyric files (TTML, the format Apple Music uses)
object Ttml {
    private fun time(raw: String?): Long? {
        if (raw == null) return null
        var t = raw.trim()
        if (t.isEmpty()) return null
        var factor = 1000.0
        if (t.endsWith("ms")) {
            factor = 1.0
            t = t.dropLast(2)
        } else if (t.endsWith("s")) {
            t = t.dropLast(1)
        }
        val parts = t.split(":")
        return try {
            var sec = 0.0
            for (p in parts) sec = sec * 60.0 + p.toDouble()
            if (parts.size == 1 && factor == 1.0) sec.toLong() else (sec * factor).toLong()
        } catch (e: Exception) {
            null
        }
    }

    private fun attr(e: Element, name: String): String? {
        if (e.hasAttribute(name)) return e.getAttribute(name)
        val attrs = e.attributes
        for (i in 0 until attrs.length) {
            val n = attrs.item(i).nodeName
            if (n.substringAfter(":") == name) return attrs.item(i).nodeValue
        }
        return null
    }

    private fun isBackground(e: Element): Boolean = attr(e, "role") == "x-bg"

    fun parse(xml: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        try {
            val f = DocumentBuilderFactory.newInstance()
            f.isNamespaceAware = false
            val doc = f.newDocumentBuilder().parse(InputSource(StringReader(xml)))
            val ps = doc.getElementsByTagName("p")
            for (i in 0 until ps.length) {
                val p = ps.item(i) as? Element ?: continue
                val words = ArrayList<LyricWord>()
                var open = false
                val kids = p.childNodes
                for (k in 0 until kids.length) {
                    val n = kids.item(k)
                    if (n.nodeType == Node.ELEMENT_NODE && n.nodeName.substringAfter(":") == "span") {
                        val sp = n as Element
                        if (isBackground(sp)) {
                            open = false
                            continue
                        }
                        val b = time(attr(sp, "begin"))
                        val e = time(attr(sp, "end"))
                        val raw = sp.textContent ?: ""
                        if (b == null || e == null) {
                            open = false
                            continue
                        }
                        if (raw.isNotEmpty() && raw.first().isWhitespace()) open = false
                        val txt = raw.trim()
                        if (txt.isEmpty()) {
                            open = false
                            continue
                        }
                        if (open && words.isNotEmpty()) {
                            val last = words.removeAt(words.size - 1)
                            words.add(LyricWord(last.startMs, e, last.text + txt))
                        } else {
                            words.add(LyricWord(b, e, txt))
                        }
                        open = !raw.last().isWhitespace()
                    } else if (n.nodeType == Node.TEXT_NODE) {
                        val t = n.nodeValue ?: ""
                        if (t.isNotEmpty() && t.any { it.isWhitespace() }) open = false
                    }
                }
                if (words.isNotEmpty()) {
                    val start = time(attr(p, "begin")) ?: words.first().startMs
                    out.add(LyricLine(minOf(start, words.first().startMs), words.joinToString(" ") { it.text }, words))
                } else {
                    // a file with whole lines only
                    val b = time(attr(p, "begin")) ?: continue
                    val txt = (p.textContent ?: "").trim()
                    if (txt.isNotEmpty()) out.add(LyricLine(b, txt))
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        val sorted = out.sortedBy { it.ms }
        // a long gap between two lines becomes an empty line, so the three dots show during the pause
        val withGaps = ArrayList<LyricLine>()
        for (i in sorted.indices) {
            withGaps.add(sorted[i])
            val w = sorted[i].words
            val endMs = if (w != null && w.isNotEmpty()) w.last().endMs else sorted[i].ms + 3000L
            if (i + 1 < sorted.size && sorted[i + 1].ms - endMs >= 6000L) withGaps.add(LyricLine(endMs + 400L, ""))
        }
        return withGaps
    }
}

/**
 * A line with real word times: each word fills when it is sung, and the word that is sung right now glows.
 * clock() gives the time of the song in ms (it moves every frame).
 */
@Composable
fun WordLyricText(
    line: LyricLine,
    on: Boolean,
    clock: () -> Long,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    center: Boolean
) {
    val words = line.words
    val dim = Color.White.copy(alpha = 0.42f)
    val body = if (!on || words == null) {
        buildAnnotatedString { append(line.text) }
    } else {
        val now = clock()
        buildAnnotatedString {
            words.forEachIndexed { i, w ->
                val len = (w.endMs - w.startMs).coerceAtLeast(80L).toFloat()
                val f = ((now - w.startMs) / len).coerceIn(0f, 1f)
                // glow: strong while the word is sung, fades away in a third of a second after it
                val glow = if (now < w.startMs) 0f else if (now <= w.endMs) 1f else (1f - (now - w.endMs) / 350f).coerceIn(0f, 1f)
                val style = if (glow > 0.01f) {
                    SpanStyle(
                        color = lerp(dim, Color.White, f),
                        shadow = Shadow(Color.White.copy(alpha = 0.85f * glow), Offset.Zero, 26f * glow)
                    )
                } else {
                    SpanStyle(color = lerp(dim, Color.White, f))
                }
                withStyle(style) { append(w.text) }
                if (i < words.lastIndex) append(" ")
            }
        }
    }
    Text(
        body,
        color = Color.White,
        fontFamily = PrismFont,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = FontWeight.Bold,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}
