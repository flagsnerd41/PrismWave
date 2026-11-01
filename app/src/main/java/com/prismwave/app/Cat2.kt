package com.prismwave.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val C_IDLE = 0
private const val C_WALK = 1
private const val C_EAT = 2
private const val C_CLIMB = 3
private const val C_NAP = 4
private const val C_DOWN = 5
private const val C_PLAY = 6

private const val TREE_X = 34f
private const val TREE_H = 34f

// What the cat is doing and where he is, all in dp.
private class CatBrain {
    var wDp = 340f
    var x = 150f
    var yUp = 0f
    var face = 1
    var state = C_IDLE
    var after = C_IDLE
    var target = 150f
    var startX = 150f
    var timer = 0f
    var idleFor = 3f
    var anim = 0f
    var hunger = 0.3f
    var bowl = 1f
    var empty = 0f
    var meow = 0f
    var hop = 0f

    private fun go(tx: Float, aft: Int) {
        target = tx.coerceIn(30f, max(40f, wDp - 20f))
        after = aft
        state = C_WALK
        timer = 0f
    }

    fun poke() {
        meow = 1.6f
        hop = 0.5f
    }

    fun refill() {
        bowl = 1f
        empty = 0f
        if (state == C_IDLE) {
            hunger = max(hunger, 0.9f)
            timer = idleFor
        }
    }

    private fun decide(playing: Boolean, night: Boolean) {
        val bowlX = wDp - 30f
        val r = Random.nextFloat()
        if (hunger > 0.8f && bowl > 0.15f) {
            go(bowlX - 36f, C_EAT)
        } else if ((!playing || night) && r < 0.7f) {
            go(TREE_X + 40f, C_CLIMB)
        } else if (r < 0.4f) {
            go(TREE_X + 34f, C_PLAY)
        } else {
            val lo = TREE_X + 70f
            val hi = bowlX - 80f
            go(if (hi > lo) lo + Random.nextFloat() * (hi - lo) else wDp * 0.5f, C_IDLE)
        }
    }

    fun update(dt: Float, playing: Boolean, night: Boolean) {
        anim += dt
        timer += dt
        if (meow > 0f) meow -= dt
        if (hop > 0f) hop -= dt
        hunger = min(1.2f, hunger + dt / 80f)
        if (bowl < 0.15f) {
            empty += dt
            if (empty > 35f) {
                bowl = 1f
                empty = 0f
            }
        }
        when (state) {
            C_IDLE -> if (timer > idleFor) decide(playing, night)
            C_WALK -> {
                val dx = target - x
                val sp = 40f * dt
                if (abs(dx) <= sp) {
                    x = target
                    state = after
                    timer = 0f
                    idleFor = 3f + Random.nextFloat() * 3f
                    if (after == C_EAT) face = 1
                    if (after == C_PLAY) face = -1
                    if (after == C_CLIMB) startX = x
                } else {
                    x += sp * (if (dx > 0f) 1f else -1f)
                    face = if (dx > 0f) 1 else -1
                }
            }
            C_EAT -> if (timer > 5f) {
                bowl = max(0f, bowl - 0.25f)
                hunger = 0f
                state = C_IDLE
                timer = 0f
                idleFor = 3f + Random.nextFloat() * 3f
            }
            C_CLIMB -> {
                val u = (timer / 0.7f).coerceIn(0f, 1f)
                yUp = TREE_H * sin(u * PI.toFloat() / 2f)
                x = startX + (TREE_X - startX) * u
                if (u >= 1f) {
                    state = C_NAP
                    timer = 0f
                }
            }
            C_NAP -> if ((playing && !night && timer > 14f) || timer > 60f) {
                state = C_DOWN
                timer = 0f
            }
            C_DOWN -> {
                val u = (timer / 0.6f).coerceIn(0f, 1f)
                yUp = TREE_H * (1f - u)
                x = TREE_X + 46f * u
                if (u >= 1f) {
                    yUp = 0f
                    state = C_IDLE
                    timer = 0f
                    idleFor = 3f + Random.nextFloat() * 3f
                }
            }
            C_PLAY -> if (timer > 4.5f) {
                state = C_IDLE
                timer = 0f
                idleFor = 3f + Random.nextFloat() * 3f
            }
        }
    }
}

@Composable
fun NavCatScene(s: AppState) {
    val ctx = LocalContext.current
    remember { LookPrefs.init(ctx); 0 }
    if (LookPrefs.catOn) {
        val d = LocalDensity.current.density
        val brain = remember { CatBrain() }
        var tick by remember { mutableStateOf(0L) }
        val still = s.reduceMotion || s.batterySaver || Opts.lite
        val hour = rememberHour()
        val night = hour >= 22f || hour < 6f
        val playingNow by rememberUpdatedState(s.playing)
        val nightNow by rememberUpdatedState(night)
        val accent = s.accent
        LaunchedEffect(still) {
            if (!still) {
                var last = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                    last = now
                    brain.update(dt, playingNow, nightNow)
                    tick = now
                }
            }
        }
        val src = remember { MutableInteractionSource() }
        val src2 = remember { MutableInteractionSource() }
        Box(
            Modifier
                .fillMaxWidth()
                .layout { m, c ->
                    val pl = m.measure(c)
                    val pull = 10.dp.roundToPx()
                    layout(pl.width, pl.height - pull) { pl.place(0, 0) }
                }
                .height(76.dp)
                .onSizeChanged { brain.wDp = it.width / d }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val ignore = tick
                val playing = playingNow
                drawScene(brain, density, playing, accent, still)
            }
            // tap the cat: meow and hop
            Box(
                Modifier
                    .offset {
                        val t0 = tick
                        IntOffset(((brain.x - 27f) * d).toInt(), ((75f - brain.yUp - 52f) * d).toInt() + (if (t0 < 0L) 1 else 0))
                    }
                    .size(54.dp)
                    .clickable(interactionSource = src, indication = null) { brain.poke() }
            )
            // tap the bowl: refill and call him
            Box(
                Modifier
                    .offset { IntOffset(((brain.wDp - 56f) * d).toInt(), (34f * d).toInt()) }
                    .size(56.dp, 42.dp)
                    .clickable(interactionSource = src2, indication = null) { brain.refill() }
            )
            // the "meow!" bubble
            Box(
                Modifier
                    .offset {
                        val t0 = tick
                        IntOffset(((brain.x - 22f) * d).toInt(), ((75f - brain.yUp - 74f) * d).toInt() + (if (t0 < 0L) 1 else 0))
                    }
                    .graphicsLayer {
                        val t0 = tick
                        alpha = if (brain.meow > 0f && t0 >= 0L) 1f else 0f
                    }
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) { Text("meow!", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun DrawScope.drawScene(b: CatBrain, d: Float, playing: Boolean, accent: Color, still: Boolean) {
    val ground = size.height - 1f * d
    val platTop = ground - TREE_H * d
    // ---- cat tree
    val wood = Color(0xFF8B6B4A)
    val rope1 = Color(0xFFD9C29A)
    val rope2 = Color(0xFFB89C70)
    val tx = TREE_X * d
    drawRoundRect(wood, Offset(tx - 20f * d, ground - 5f * d), Size(40f * d, 5f * d), androidx.compose.ui.geometry.CornerRadius(2f * d))
    drawRect(rope1, Offset(tx - 4.5f * d, platTop), Size(9f * d, TREE_H * d - 5f * d))
    var ry = platTop + 2f * d
    while (ry < ground - 6f * d) {
        drawLine(rope2, Offset(tx - 4.5f * d, ry), Offset(tx + 4.5f * d, ry + 2f * d), strokeWidth = 1.2f * d)
        ry += 4f * d
    }
    drawRoundRect(wood, Offset(tx - 18f * d, platTop - 1f * d), Size(36f * d, 6f * d), androidx.compose.ui.geometry.CornerRadius(3f * d))
    drawRoundRect(Color(0xFF5C7A89), Offset(tx - 17f * d, platTop - 3f * d), Size(34f * d, 3.5f * d), androidx.compose.ui.geometry.CornerRadius(2f * d))
    // hanging ball
    val sw = if (still) 0f else sin(b.anim * 2.2f) * 0.25f + (if (b.state == C_PLAY) sin(b.anim * 7f) * 0.5f else 0f)
    val hx0 = tx + 16f * d
    val hy0 = platTop + 4f * d
    val len = 20f * d
    val bx = hx0 + sin(sw) * len
    val by = hy0 + cos(sw) * len
    drawLine(Color.White.copy(alpha = 0.6f), Offset(hx0, hy0), Offset(bx, by), strokeWidth = 1f * d)
    drawCircle(Color(0xFFE0652B), 4.5f * d, Offset(bx, by))
    drawCircle(Color.White.copy(alpha = 0.5f), 1.4f * d, Offset(bx - 1.5f * d, by - 1.5f * d))
    // ---- food bowl
    val bwx = size.width - 30f * d
    val bowlP = Path()
    bowlP.moveTo(bwx - 14f * d, ground - 9f * d)
    bowlP.lineTo(bwx + 14f * d, ground - 9f * d)
    bowlP.quadraticBezierTo(bwx + 12f * d, ground, bwx, ground)
    bowlP.quadraticBezierTo(bwx - 12f * d, ground, bwx - 14f * d, ground - 9f * d)
    bowlP.close()
    drawPath(bowlP, Color(0xFF4F7FE0))
    drawOval(Color(0xFF7FA8FF), Offset(bwx - 15f * d, ground - 11f * d), Size(30f * d, 5f * d))
    val food = (b.bowl * 7f).toInt()
    for (i in 0 until food) {
        val fx = bwx + ((i % 4) - 1.5f) * 6f * d
        val fy = ground - 10f * d - (i / 4) * 3.5f * d
        drawCircle(Color(0xFF8B5A2B), 2.4f * d, Offset(fx, fy))
    }
    // ---- the cat
    val u = d * 0.62f
    var bodyRot = 0f
    var bob = 0f
    var headDx = 0f
    var headDy = 0f
    var headRot = 0f
    var eyes = 0
    var pawUp = 0f
    var pawSide = if (b.face >= 0) 1 else -1
    var mouth = if (b.meow > 0f) 1f else 0f
    val t = b.anim
    val tailSw = if (still) 0f else sin(t * (if (playing) 4.5f else 2f))
    var blink = false
    when (b.state) {
        C_WALK -> {
            bodyRot = 6f * sin(t * 12f)
            bob = -2.5f * abs(sin(t * 12f))
        }
        C_EAT -> {
            headDy = 9f + 2.5f * abs(sin(t * 8f))
            headDx = 8f * b.face
            headRot = 18f * b.face
            eyes = 1
        }
        C_CLIMB -> bob = -8f * sin(min(1f, b.timer / 0.7f) * PI.toFloat())
        C_DOWN -> bob = -8f * sin(min(1f, b.timer / 0.6f) * PI.toFloat())
        C_NAP -> {
            eyes = 1
            headDy = 3f
            headRot = 6f
            bob = 0.6f * sin(t * 1.6f)
        }
        C_PLAY -> {
            eyes = 2
            headRot = -8f
            pawSide = -1
            pawUp = max(0f, sin(t * 7f))
        }
        else -> {
            val ph = (b.timer % 7f) / 7f
            if (ph > 0.5f && ph < 0.8f) {
                pawUp = 1f
                headRot = 10f
                eyes = 1
            }
            blink = ph > 0.2f && ph < 0.23f
        }
    }
    if (playing && (b.state == C_IDLE || b.state == C_WALK)) headDy += 1.3f * sin(t * 2f * PI.toFloat() * 1.7f)
    if (b.hop > 0f) bob -= 14f * sin((1f - b.hop / 0.5f) * PI.toFloat())
    val cx = b.x * d
    val gy = ground - b.yUp * d
    drawOrangeCat(cx, gy, u, bodyRot, bob, headDx, headDy, headRot, eyes, blink, tailSw, pawUp, pawSide, mouth)
    // sleepy z's
    if (b.state == C_NAP) {
        for (i in 0 until 3) {
            val ph = ((t * 0.5f + i / 3f) % 1f)
            val zs = (4f + i * 2f) * d
            val zx = cx + (14f + ph * 12f) * d
            val zy = gy - (36f + ph * 26f) * d
            val za = sin(ph * PI.toFloat()).coerceIn(0f, 1f)
            val z = Path()
            z.moveTo(zx, zy)
            z.lineTo(zx + zs, zy)
            z.lineTo(zx, zy + zs)
            z.lineTo(zx + zs, zy + zs)
            drawPath(z, Color.White.copy(alpha = 0.85f * za), style = Stroke(width = 1.5f * d, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawOrangeCat(
    cx: Float,
    groundY: Float,
    k: Float,
    bodyRot: Float,
    bob: Float,
    headDx: Float,
    headDy: Float,
    headRot: Float,
    eyes: Int,
    blink: Boolean,
    tailSw: Float,
    pawUp: Float,
    pawSide: Int,
    mouth: Float
) {
    val orange = Color(0xFFF5A21B)
    val orangeD = Color(0xFFD98410)
    val belly = Color(0xFF9B6B1F)
    val paw = Color(0xFFFFB84D)
    val dark = Color(0xFF3A2208)
    withTransform({
        translate(cx - 50f * k, groundY - 78f * k + bob * k)
        scale(k, k, Offset.Zero)
        rotate(bodyRot, Offset(50f, 76f))
    }) {
        // tail
        val tail = Path()
        tail.moveTo(76f, 72f)
        tail.quadraticBezierTo(104f, 74f + tailSw * 4f, 96f + tailSw * 6f, 46f)
        drawPath(tail, orangeD, style = Stroke(width = 7f, cap = StrokeCap.Round))
        // body, belly patch, back paws
        drawOval(orange, Offset(17f, 28f), Size(66f, 50f))
        drawOval(belly, Offset(33f, 42f), Size(34f, 34f))
        drawOval(paw, Offset(28f, 72f), Size(15f, 7f))
        drawOval(paw, Offset(57f, 72f), Size(15f, 7f))
        // front paws (one can lift up to lick or swat)
        for (side in intArrayOf(-1, 1)) {
            if (side == pawSide && pawUp > 0.01f) {
                val px = 50f + (9f + pawUp * 3f) * side
                val py = 74f - 40f * pawUp
                drawLine(orange, Offset(50f + 14f * side, 56f), Offset(px, py), strokeWidth = 8f, cap = StrokeCap.Round)
                drawCircle(paw, 4.8f, Offset(px, py))
            } else {
                drawOval(paw, Offset(50f + 9f * side - 7f, 69f), Size(14f, 8f))
            }
        }
        // head
        withTransform({
            translate(headDx, headDy)
            rotate(headRot, Offset(50f, 40f))
        }) {
            drawOval(orange, Offset(27f, 10f), Size(46f, 38f))
            val ear1 = Path()
            ear1.moveTo(30f, 22f)
            ear1.lineTo(33f, 3f)
            ear1.lineTo(46f, 13f)
            ear1.close()
            val ear2 = Path()
            ear2.moveTo(54f, 13f)
            ear2.lineTo(67f, 3f)
            ear2.lineTo(70f, 22f)
            ear2.close()
            drawPath(ear1, orange)
            drawPath(ear2, orange)
            val in1 = Path()
            in1.moveTo(33f, 19f)
            in1.lineTo(34.5f, 8f)
            in1.lineTo(42f, 14f)
            in1.close()
            val in2 = Path()
            in2.moveTo(58f, 14f)
            in2.lineTo(65.5f, 8f)
            in2.lineTo(67f, 19f)
            in2.close()
            drawPath(in1, Color(0xFF8B5A2B))
            drawPath(in2, Color(0xFF8B5A2B))
            // forehead stripes
            drawLine(orangeD, Offset(50f, 12f), Offset(50f, 20f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            drawLine(orangeD, Offset(44f, 13f), Offset(45f, 19f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            drawLine(orangeD, Offset(56f, 13f), Offset(55f, 19f), strokeWidth = 1.8f, cap = StrokeCap.Round)
            // eyes: half closed and a bit grumpy, closed, or wide
            for (ex in floatArrayOf(41f, 59f)) {
                if (eyes == 1) {
                    val arc = Path()
                    arc.moveTo(ex - 4.5f, 29.5f)
                    arc.quadraticBezierTo(ex, 33f, ex + 4.5f, 29.5f)
                    drawPath(arc, dark, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
                } else {
                    drawOval(Color.White, Offset(ex - 5.5f, 24.5f), Size(11f, 9.5f))
                    drawCircle(dark, 2.6f, Offset(ex, 30f))
                    if (blink) {
                        drawRect(orange, Offset(ex - 6f, 23.5f), Size(12f, 11f))
                    } else if (eyes == 0) {
                        drawRect(orange, Offset(ex - 6f, 23.5f), Size(12f, 5.2f))
                        drawLine(dark, Offset(ex - 5.5f, 28.7f), Offset(ex + 5.5f, 28.7f), strokeWidth = 1.3f)
                    }
                }
            }
            // nose, mouth, whiskers
            val nose = Path()
            nose.moveTo(47.5f, 35f)
            nose.lineTo(52.5f, 35f)
            nose.lineTo(50f, 38f)
            nose.close()
            drawPath(nose, dark)
            if (mouth > 0.1f) {
                drawOval(Color(0xFF5A1F1F), Offset(47.5f, 38.5f), Size(5f, 4.5f * mouth))
            } else {
                drawLine(dark, Offset(50f, 38f), Offset(50f, 40f), strokeWidth = 1f)
            }
            val wh = Color.White.copy(alpha = 0.6f)
            drawLine(wh, Offset(40f, 36f), Offset(24f, 34f), strokeWidth = 0.9f)
            drawLine(wh, Offset(40f, 38f), Offset(24f, 40f), strokeWidth = 0.9f)
            drawLine(wh, Offset(60f, 36f), Offset(76f, 34f), strokeWidth = 0.9f)
            drawLine(wh, Offset(60f, 38f), Offset(76f, 40f), strokeWidth = 0.9f)
        }
    }
}
