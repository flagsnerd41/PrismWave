package com.prismwave.app

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Own on/off switch for the experimental liquid glass bar (Android 13+). */
object LiquidGlassOpt {
    var on by mutableStateOf(true)
    private var loaded = false
    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        on = ctx.getSharedPreferences("prismwave", Context.MODE_PRIVATE).getBoolean("liquidGlass", true)
    }
    fun set(ctx: Context, v: Boolean) {
        on = v
        ctx.getSharedPreferences("prismwave", Context.MODE_PRIVATE).edit().putBoolean("liquidGlass", v).apply()
    }
}

private const val LIQUID_AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float depth;

float sdf(float2 p) {
    float2 c = size * 0.5;
    float2 q = abs(p - c) - (c - float2(radius));
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

half4 main(float2 p) {
    float d = sdf(p);
    if (d > 0.0) { return half4(0.0); }
    float e = 1.0;
    float2 g = float2(sdf(p + float2(e, 0.0)) - sdf(p - float2(e, 0.0)),
                      sdf(p + float2(0.0, e)) - sdf(p - float2(0.0, e)));
    float2 n = g / max(length(g), 0.0001);
    float rim = clamp(1.0 + d / depth, 0.0, 1.0);
    float bend = rim * rim;
    float2 lo = float2(0.5);
    float2 hi = size - float2(0.5);
    half4 cr = content.eval(clamp(p - n * bend * depth * 0.80, lo, hi));
    half4 cg = content.eval(clamp(p - n * bend * depth * 0.95, lo, hi));
    half4 cb = content.eval(clamp(p - n * bend * depth * 1.10, lo, hi));
    half3 col = half3(cr.r, cg.g, cb.b);
    col = mix(col, half3(1.0), 0.07);
    float light = 0.5 + 0.5 * dot(n, normalize(float2(-0.5, -0.85)));
    float edge = pow(rim, 3.0);
    col += half3(edge * (0.10 + 0.40 * light));
    float aa = clamp(0.5 - d, 0.0, 1.0);
    return half4(col * half(aa), half(aa));
}
"""

private class LiquidCache {
    var w = 0f
    var h = 0f
    var b = 0f
}

/** Real liquid glass (edge refraction + chromatic split) on Android 13+, otherwise the normal blur glass. */
@Composable
fun LiquidGlass(
    alpha: Float,
    modifier: Modifier = Modifier,
    radius: Dp = 30.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val backdrop = LocalBackdrop.current
    if (Build.VERSION.SDK_INT >= 33 && LiquidGlassOpt.on && backdrop != null && backdrop.enabled) {
        LiquidGlassImpl(backdrop, modifier, radius, content)
    } else {
        Glass(alpha, modifier, RoundedCornerShape(radius), content)
    }
}

@RequiresApi(33)
@Composable
private fun LiquidGlassImpl(
    backdrop: Backdrop,
    modifier: Modifier,
    radius: Dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val layer = rememberGraphicsLayer()
    val shader = remember { RuntimeShader(LIQUID_AGSL) }
    val cache = remember { LiquidCache() }
    var pos by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .clip(shape)
            .then(modifier)
            .onGloballyPositioned { pos = it.positionInRoot() }
            .drawBehind {
                if (backdrop.size.width > 0) {
                    val w = size.width
                    val h = size.height
                    val bl = (backdrop.radius * 0.18f).coerceIn(2f, 40f)
                    if (cache.w != w || cache.h != h || cache.b != bl) {
                        cache.w = w
                        cache.h = h
                        cache.b = bl
                        shader.setFloatUniform("size", w, h)
                        shader.setFloatUniform("radius", radius.toPx())
                        shader.setFloatUniform("depth", minOf(22.dp.toPx(), h * 0.4f))
                        val blur = RenderEffect.createBlurEffect(bl, bl, Shader.TileMode.CLAMP)
                        val fx = RenderEffect.createRuntimeShaderEffect(shader, "content")
                        layer.renderEffect = RenderEffect.createChainEffect(fx, blur).asComposeRenderEffect()
                    }
                    layer.clip = true
                    layer.record(this, layoutDirection, IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1))) {
                        translate(-pos.x, -pos.y) { drawLayer(backdrop.layer) }
                    }
                    drawLayer(layer)
                }
            }
            .background(Color.White.copy(alpha = 0.03f))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.30f))
                ),
                shape
            ),
        content = content
    )
}
