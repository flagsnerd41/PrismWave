package com.prismwave.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Which step the now playing screen is on: false = the player, true = the half lyrics view.
// The third step is the full lyrics page (Pages "lyrics").
object PlayerStage {
    var half by mutableStateOf(false)
}

/**
 * The half view, like the very first PrismWave player: the cover fills the top, its bottom
 * melts into a blur, a small header with the title and a pause button sits on it, and the
 * lyrics run underneath. p() is 0 to 1 while you drag it in.
 */
@Composable
fun HalfLyricsView(s: AppState, song: Song, p: () -> Float) {
    val ctx = LocalContext.current
    Lyrics.init(ctx)
    val artRaw = rememberArtColor(song)
    val art by animateColorAsState(artRaw, tween(700), label = "halfArt")
    var lines by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var loaded by remember(song.id) { mutableStateOf(false) }
    LaunchedEffect(song.id) {
        lines = Lyrics.fetch(song)
        loaded = true
    }
    val synced = lines.isNotEmpty() && lines[0].ms >= 0
    val posMs = (s.position * 1000).toLong()
    val idx = if (synced) lines.indexOfLast { it.ms <= posMs + 250 } else -1
    val base = lerp(art, Color.Black, 0.55f)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val v = p()
                alpha = (v * 1.5f).coerceIn(0f, 1f)
                translationY = (1f - v) * size.height * 0.30f
            }
            .background(base)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
    ) {
        SwirlBackground(art, s.reduceMotion || Opts.lite)

        // the cover on top, with the bottom melting into a blur
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.56f).shareCover(1)) {
            Cover(song, Modifier.fillMaxSize(), 0.dp, 160.sp, quality = 1200)
            if (!Opts.lite) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.45f to Color.Transparent,
                                    0.80f to Color.Black
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    Cover(song, Modifier.fillMaxSize().blur(26.dp), 0.dp, 160.sp, quality = 600)
                }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.60f to Color.Transparent, 1f to base.copy(alpha = 0.92f))
                )
            )
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                RoundBtn(if (s.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 0.9f, { s.toggle() }, 40.dp)
            }
        }

        // the lyrics: the line that is playing is clear, the next ones fade and blur away
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(0.44f)
                .navigationBarsPadding()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    Pages.show("lyrics")
                }
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (lines.isEmpty()) {
                Text(
                    if (loaded) "No lyrics found for this song" else "Loading lyrics...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )
            } else {
                AnimatedContent(
                    targetState = idx,
                    transitionSpec = {
                        (slideInVertically(tween(450, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(350))) togetherWith
                            (slideOutVertically(tween(350, easing = FastOutSlowInEasing)) { -it / 3 } + fadeOut(tween(250)))
                    },
                    label = "halfLines"
                ) { i ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (k in 0 until 4) {
                            val ln = lines.getOrNull(if (i >= 0) i + k else k)
                            if (ln != null) {
                                Text(
                                    ln.text.ifEmpty { "..." },
                                    color = Color.White,
                                    fontSize = if (k == 0) 27.sp else 25.sp,
                                    lineHeight = 33.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .graphicsLayer { alpha = if (k == 0) 1f else (0.58f - 0.16f * k).coerceAtLeast(0.14f) }
                                        .then(if (k > 0 && !Opts.lite) Modifier.blur((k * 1.6f).dp) else Modifier)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
