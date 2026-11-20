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
        return withPauseDots(out.sortedBy { it.ms })
    }
}

// an instrumental part (long gap between two lines, or a long intro) becomes an empty line: the three dots show there
fun withPauseDots(lines: List<LyricLine>): List<LyricLine> {
    if (lines.isEmpty() || lines[0].ms < 0) return lines
    val out = ArrayList<LyricLine>()
    if (lines[0].text.isNotEmpty() && lines[0].ms >= 5000L) out.add(LyricLine(500L, ""))
    for (i in lines.indices) {
        val l = lines[i]
        out.add(l)
        if (l.text.isEmpty() || i + 1 >= lines.size || lines[i + 1].text.isEmpty()) continue
        val w = l.words
        val endMs = if (w != null && w.isNotEmpty()) w.last().endMs else l.ms + (l.text.length * 95L).coerceIn(1800L, 6000L)
        if (lines[i + 1].ms - endMs >= 4500L) out.add(LyricLine(endMs + 400L, ""))
    }
    return out
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
    val dim = Color.White.copy(alpha = 0.38f)
    val body = if (!on || words == null) {
        buildAnnotatedString { append(line.text) }
    } else {
        val now = clock()
        buildAnnotatedString {
            words.forEachIndexed { i, w ->
                val len = (w.endMs - w.startMs).coerceAtLeast(120L).toFloat()
                val f = ((now - w.startMs) / len).coerceIn(0f, 1f)
                val n = w.text.length.coerceAtLeast(1)
                val edge = 3.2f
                for (c in 0 until n) {
                    // each letter fills one after the other with a wide soft edge (smoothstep), no glow, no shadow
                    val raw = ((f * (n + edge) - c) / edge).coerceIn(0f, 1f)
                    val cf = raw * raw * (3f - 2f * raw)
                    withStyle(SpanStyle(color = lerp(dim, Color.White, cf))) { append(w.text[c].toString()) }
                }
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
