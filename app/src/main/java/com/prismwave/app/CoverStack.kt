package com.prismwave.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * A 3D stack of covers. The cover in the middle is big and flat, the rest lie stacked above
 * and below it. Drag up or down to flip through the stack, tap the middle cover to play it.
 */
@Composable
fun CoverStackScreen(s: AppState) {
    BackHandler { Pages.close() }
    var src by remember { mutableStateOf(0) }
    val list = when (src) {
        0 -> s.queue
        1 -> s.history.toList()
        else -> s.liked.toList()
    }.distinctBy { it.id }.take(60)
    val last = (list.size - 1).coerceAtLeast(0)
    val pos = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(src, list.size) {
        val i = list.indexOfFirst { it.id == s.current?.id }
        pos.snapTo(if (i >= 0) i.toFloat() else 0f)
    }
    val focus by remember(list) { derivedStateOf { pos.value.roundToInt().coerceIn(0, last) } }
    val focused = list.getOrNull(focus)
    val art = if (focused != null) rememberArtColor(focused) else Night

    Box(Modifier.fillMaxSize().background(Night)) {
        SwirlBackground(art, s.reduceMotion || Opts.lite)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    listOf("Queue", "History", "Liked").forEachIndexed { i, label ->
                        Chip(label, src == i, s.accent) { src = i }
                    }
                }
                RoundBtn(Icons.Rounded.Close, 0.9f, { Pages.close() }, 44.dp)
            }
            if (list.isEmpty()) {
                Text(
                    "Nothing here yet",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(40.dp)
                )
            } else {
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .draggable(
                            state = rememberDraggableState { d ->
                                scope.launch { pos.snapTo((pos.value - d / 120f).coerceIn(0f, last.toFloat())) }
                            },
                            orientation = Orientation.Vertical,
                            onDragStopped = { vel ->
                                val target = (pos.value - vel / 900f).roundToInt().coerceIn(0, last)
                                pos.animateTo(target.toFloat(), spring(dampingRatio = 0.8f, stiffness = 180f))
                            }
                        )
                ) {
                    val big = maxWidth * 0.78f
                    val density = LocalDensity.current.density
                    list.forEachIndexed { i, sg ->
                        if (abs(i - focus) <= 8) {
                            key(sg.id) {
                                Box(
                                    Modifier
                                        .align(Alignment.Center)
                                        .size(big)
                                        .zIndex(-abs(i - focus).toFloat())
                                        .graphicsLayer {
                                            val d = i - pos.value
                                            val a = d.coerceIn(-1f, 1f)
                                            val ad = abs(d)
                                            val bigPx = big.toPx()
                                            val edge = bigPx / 2f + 0.17f * bigPx
                                            val step = 30.dp.toPx()
                                            translationY = if (ad <= 1f) {
                                                d * edge
                                            } else {
                                                (if (d < 0f) -1f else 1f) * (edge + (ad - 1f) * step)
                                            }
                                            rotationX = -68f * a
                                            cameraDistance = 14f * density
                                            val sc = 1f - 0.04f * ad.coerceAtMost(4f)
                                            scaleX = sc
                                            scaleY = sc
                                            alpha = (1.25f - 0.13f * ad).coerceIn(0.2f, 1f)
                                        }
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                                        .clickable {
                                            if (i == focus) {
                                                s.play(sg, list)
                                                Pages.close()
                                            } else {
                                                scope.launch {
                                                    pos.animateTo(i.toFloat(), spring(dampingRatio = 0.8f, stiffness = 200f))
                                                }
                                            }
                                        }
                                ) {
                                    Cover(sg, Modifier.fillMaxSize(), 0.dp, 60.sp, quality = 600)
                                }
                            }
                        }
                    }
                }
                if (focused != null) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            focused.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            focused.artist,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(1.dp))
                        Box(
                            Modifier
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .clickable {
                                    s.play(focused, list)
                                    Pages.close()
                                }
                                .padding(horizontal = 30.dp, vertical = 10.dp)
                        ) {
                            Text("Play", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
