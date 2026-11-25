package com.prismwave.app

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------ how the full player opens

// 0 = Expand from mini-player (the player grows out of the mini player, like it shrinks back when closing)
// 1 = Cover flight (the small cover flies to the big cover)
object OpenAnim {
    var style by mutableStateOf(0)
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        style = ctx.getSharedPreferences("prismwave", 0).getInt("openAnim", 0)
    }

    fun set(ctx: Context, v: Int) {
        style = v
        ctx.getSharedPreferences("prismwave", 0).edit().putInt("openAnim", v).apply()
    }
}

// ------------------------------------------------------------ liquid glass icons

object GlassIcons {
    var on by mutableStateOf(true)
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        on = ctx.getSharedPreferences("prismwave", 0).getBoolean("glassIcons", true)
    }

    fun set(ctx: Context, v: Boolean) {
        on = v
        ctx.getSharedPreferences("prismwave", 0).edit().putBoolean("glassIcons", v).apply()
    }
}

/**
 * Draws an icon as frosted glass: the shape of the icon shows a blurred copy of what is
 * behind it under a white tint, so it reads as a white icon with blur inside. It moves with the content, so the colors change live.
 */
fun Modifier.liquidGlyph(): Modifier = composed {
    val backdrop = LocalBackdrop.current
    if (!GlassIcons.on || backdrop == null || !backdrop.enabled || Build.VERSION.SDK_INT < 31 || Opts.lite) {
        return@composed this
    }
    val layer = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { pos = it.positionInRoot() }
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            // the icon itself is only the shape
            drawContent()
            if (backdrop.size.width > 0) {
                val w = size.width.toInt().coerceAtLeast(1)
                val h = size.height.toInt().coerceAtLeast(1)
                layer.blendMode = BlendMode.SrcIn
                layer.renderEffect = BlurEffect(26f, 26f, TileMode.Clamp)
                layer.colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(2.0f) })
                layer.record(this, layoutDirection, IntSize(w, h)) {
                    val cx = pos.x + w / 2f
                    val cy = pos.y + h / 2f
                    // the lens: the background seen 2.6x bigger around the icon, so the colors bend inside it
                    scale(1.8f, pivot = Offset(cx, cy)) {
                        translate(-pos.x, -pos.y) { drawLayer(backdrop.layer) }
                    }
                    drawRect(Color.White.copy(alpha = 0.30f))
                }
                drawLayer(layer)
            }
        }
}

// ------------------------------------------------------------ compact iOS style Now Playing menu

class IosItem(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val sub: String?, val run: () -> Unit)

@Composable
private fun IosGroup(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
    ) { content() }
}

@Composable
private fun IosQuick(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, on: Boolean, accent: Color, modifier: Modifier, run: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .softClick(run)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = if (on) accent else Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun IosList(items: List<IosItem>) {
    IosGroup {
        items.forEachIndexed { i, it ->
            Row(
                Modifier.fillMaxWidth().softClick(it.run).padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(it.icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(it.label, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (it.sub != null) Text(it.sub, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (i < items.lastIndex) {
                Box(Modifier.fillMaxWidth().padding(start = 45.dp).height(0.5.dp).background(Color.White.copy(alpha = 0.14f)))
            }
        }
    }
}

/**
 * The body of the Now Playing popup: three quick actions on top, then short lists with thin separators.
 * Every action of the old menu is still here.
 */
@Composable
fun IosMenuBody(
    s: AppState,
    song: Song,
    open: Float,
    showVolume: Boolean,
    volume: @Composable () -> Unit,
    liked: Boolean,
    downloaded: Boolean,
    downloadProgress: Float?,
    onDownload: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    library: List<IosItem>,
    more: List<IosItem>,
    tools: List<IosItem>
) {
    // each part rises in one after the other
    @Composable
    fun Step(i: Int, content: @Composable () -> Unit) {
        val p = ((open - 0.08f - i * 0.07f) / 0.45f).coerceIn(0f, 1f)
        Box(Modifier.graphicsLayer { alpha = p; translationY = (1f - p) * 22f }) { content() }
    }
    Step(0) {
        IosGroup {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                IosQuick(
                    if (downloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                    if (downloaded) "Remove" else if (downloadProgress != null) (downloadProgress * 100).toInt().toString() + "%" else "Download",
                    downloaded, s.accent, Modifier.weight(1f), onDownload
                )
                IosQuick(
                    if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (liked) "Unlike" else "Favorite",
                    liked, s.accent, Modifier.weight(1f), onLike
                )
                IosQuick(Icons.Rounded.Share, "Share", false, s.accent, Modifier.weight(1f), onShare)
            }
        }
    }
    if (showVolume) Step(1) { volume() }
    Step(2) { IosList(library) }
    Step(3) { IosList(more) }
    Step(4) { IosList(tools) }
}
