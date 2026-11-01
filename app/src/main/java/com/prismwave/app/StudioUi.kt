package com.prismwave.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------- Star Struck (night sky, pines, flying sparks)

private const val STAR_DUR = 8.6f

private class StarSpark(
    val delay: Float,
    val life: Float,
    val y0: Float,
    val y1: Float,
    val len: Float,
    val wid: Float
)

// Only exists while you are on Home. Plays shortly after opening, then again every minute or so.
@Composable
fun StarStruckHost(s: AppState) {
    if (s.screen == Screen.Home && StudioPrefs.starOn && !s.batterySaver && !s.reduceMotion && !s.showPlayer) {
        StarStruckOverlay(Modifier.fillMaxSize())
    }
}

@Composable
fun StarStruckOverlay(modifier: Modifier = Modifier) {
    var t by remember { mutableStateOf(-1f) }
    LaunchedEffect(Unit) {
        val first = StudioPrefs.lastStar == 0L
        val since = System.currentTimeMillis() - StudioPrefs.lastStar
        delay(if (first) 3000L else (60000L - since).coerceAtLeast(600L))
        while (true) {
            StudioPrefs.lastStar = System.currentTimeMillis()
            val start = withFrameNanos { it }
            var el = 0f
            while (el < STAR_DUR) {
                val now = withFrameNanos { it }
                el = (now - start) / 1_000_000_000f
                t = el
            }
            t = -1f
            delay(50000L)
        }
    }
    val stars = remember {
        val r = Random(11)
        List(46) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.62f, r.nextFloat() * 6.28f, 0.8f + r.nextFloat() * 1.6f) }
    }
    val sparks = remember {
        val r = Random(5)
        List(18) {
            StarSpark(
                r.nextFloat() * 5.2f,
                2.0f + r.nextFloat() * 1.4f,
                0.55f + r.nextFloat() * 0.45f,
                0.05f + r.nextFloat() * 0.5f,
                60f + r.nextFloat() * 90f,
                7f + r.nextFloat() * 9f
            )
        }
    }
    val leaves = remember {
        val r = Random(9)
        List(10) { floatArrayOf(0.2f + r.nextFloat() * 0.9f, r.nextFloat(), 0.6f + r.nextFloat() * 1.2f, 10f + r.nextFloat() * 16f, r.nextFloat() * 180f) }
    }
    if (t >= 0f) {
        Canvas(modifier) {
            val w = size.width
            val h = size.height
            val a = min(min(t / 1.2f, (STAR_DUR - t) / 1.6f), 1f).coerceIn(0f, 1f)
            // purple-blue night sky
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF4A48C8).copy(alpha = 0.34f * a),
                        Color(0xFF1B1745).copy(alpha = 0.36f * a),
                        Color(0xFF07060F).copy(alpha = 0.32f * a)
                    )
                )
            )
            // twinkling stars
            for (st in stars) {
                val tw = 0.35f + 0.65f * abs(sin(t * 1.6f + st[2]))
                drawCircle(
                    Color.White.copy(alpha = (0.9f * tw * a).coerceIn(0f, 1f)),
                    radius = st[3] * 1.4f,
                    center = Offset(st[0] * w, st[1] * h)
                )
            }
            // warm glow on the right and behind the trees
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFD9591B).copy(alpha = 0.30f * a))))
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xFFB5471A).copy(alpha = 0.28f * a)),
                    startY = h * 0.6f,
                    endY = h
                )
            )
            // two rows of pine trees
            val xs = floatArrayOf(0.03f, 0.14f, 0.26f, 0.4f, 0.52f, 0.66f, 0.79f, 0.9f, 1.0f)
            val hs = floatArrayOf(0.5f, 0.36f, 0.46f, 0.3f, 0.4f, 0.34f, 0.5f, 0.38f, 0.46f)
            for (row in 0..1) {
                val col = if (row == 0) Color(0xFF15123F).copy(alpha = 0.45f * a) else Color(0xFF07060F).copy(alpha = 0.55f * a)
                for (i in xs.indices) {
                    val cx = (xs[i] + (if (row == 0) 0.05f else 0f)) * w + sin(t * 0.9f + i) * 3f
                    val th = hs[i] * h * 0.55f * (if (row == 0) 0.8f else 1f)
                    val baseY = h * 1.03f
                    val tw = w * 0.13f
                    for (k in 0..3) {
                        val yTop = baseY - th + k * th * 0.2f
                        val yBot = yTop + th * 0.38f
                        val hw = tw * (0.38f + 0.2f * k)
                        val p = Path()
                        p.moveTo(cx, yTop)
                        p.lineTo(cx + hw, yBot)
                        p.lineTo(cx - hw, yBot)
                        p.close()
                        drawPath(p, col)
                    }
                }
            }
            // dark leaves drifting at the top left
            for (lf in leaves) {
                val x = lf[0] * w - t * lf[2] * 20f
                val y = lf[1] * h * 0.5f + t * lf[2] * 9f
                rotate(lf[4] + t * 25f, Offset(x, y)) {
                    drawOval(
                        Color(0xFF0B0A22).copy(alpha = 0.8f * a),
                        Offset(x - lf[3], y - lf[3] * 0.45f),
                        Size(lf[3] * 2f, lf[3] * 0.9f)
                    )
                }
            }
            // glowing orange sparks flying from the right
            for (sp in sparks) {
                val u = (t - sp.delay) / sp.life
                if (u >= 0f && u <= 1f) {
                    val x = w * (1.15f - 1.3f * u)
                    val y = h * (sp.y0 + (sp.y1 - sp.y0) * u)
                    val ang = atan2((sp.y1 - sp.y0) * h, -1.3f * w) * 57.29578f
                    val al = sin(PI.toFloat() * u) * a
                    rotate(ang, Offset(x, y)) {
                        drawOval(
                            Color(0xFFFF8A2B).copy(alpha = (0.85f * al).coerceIn(0f, 1f)),
                            Offset(x - sp.len / 2f, y - sp.wid / 2f),
                            Size(sp.len, sp.wid)
                        )
                        drawOval(
                            Color(0xFFFFE27A).copy(alpha = (0.9f * al).coerceIn(0f, 1f)),
                            Offset(x - sp.len * 0.3f, y - sp.wid * 0.25f),
                            Size(sp.len * 0.6f, sp.wid * 0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- string lights around anything (logo, picture)

@Composable
fun StringLightsFrame(size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        content()
        if (StudioPrefs.lightsOn) {
            val tr = rememberInfiniteTransition(label = "lights")
            val ph by tr.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
                label = "ph"
            )
            Canvas(Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val n = 9
                val pts = (0..n).map { i ->
                    val f = i / n.toFloat()
                    Offset(w * (-0.02f + 1.04f * f), h * (0.04f + 0.12f * sin(PI.toFloat() * f)))
                }
                for (i in 0 until n) drawLine(Color(0x99000000), pts[i], pts[i + 1], 2.5f)
                for (i in pts.indices) {
                    val p = pts[i]
                    val fl = 0.6f + 0.4f * sin(2f * PI.toFloat() * (ph + i * 0.17f))
                    val c = if (i % 2 == 0) Color(0xFFFFE08A) else Color(0xFFFFC46B)
                    val bulb = Offset(p.x, p.y + w * 0.025f)
                    drawCircle(
                        Brush.radialGradient(listOf(c.copy(alpha = 0.55f * fl), Color.Transparent), center = bulb, radius = w * 0.09f),
                        radius = w * 0.09f,
                        center = bulb
                    )
                    drawCircle(c.copy(alpha = fl), radius = w * 0.022f, center = bulb)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- button + page

@Composable
fun BoxScope.StudioHost(s: AppState) {
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(s.playing) { Ambient.setPlaying(s.playing) }
    if (!open && !s.showPlayer) {
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 190.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, s.accent.copy(alpha = 0.6f), CircleShape)
                .clickable { open = true },
            contentAlignment = Alignment.Center
        ) { Text("\u2728", fontSize = 20.sp) }
    }
    AnimatedVisibility(
        visible = open,
        enter = slideInVertically(tween(300)) { it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200))
    ) { StudioPage(s) { open = false } }
}

@Composable
private fun StudioChip(text: String, on: Boolean, acc: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (on) acc else CardBg2)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (on) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    acc: Color,
    show: String,
    onChange: (Float) -> Unit,
    onDone: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(show, color = TextDim, fontSize = 13.sp)
    }
    Slider(
        value = value,
        onValueChange = onChange,
        onValueChangeFinished = onDone,
        valueRange = range,
        colors = SliderDefaults.colors(
            thumbColor = acc,
            activeTrackColor = acc,
            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
        )
    )
}

@Composable
private fun StudioSection(title: String) {
    Spacer(Modifier.height(22.dp))
    Text(title, color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun StudioPage(s: AppState, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("Tell me how the song should sound or feel.") }
    val acc = s.accent
    LaunchedEffect(Unit) {
        SoundFx.speed = s.tempo
        SoundFx.pitch = s.pitch
    }
    BackHandler { onClose() }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Night, Surface1)))
            .pointerInput(Unit) { detectTapGestures { } }
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(CardBg2).clickable { onClose() },
                contentAlignment = Alignment.Center
            ) { Text("\u2715", color = Color.White, fontSize = 16.sp) }
            Spacer(Modifier.width(12.dp))
            Text("AI Sound Studio", color = Color.White, fontFamily = PrismFont, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            placeholder = { Text("e.g. I'm sad, make it muffled and add crickets", color = TextDim) }
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            val ex = listOf(
                "Make it sound like a stadium",
                "Muffled with crickets",
                "Rainy night, slowed down",
                "Underwater",
                "Old radio",
                "Cozy fireplace"
            )
            ex.forEach { StudioChip(it, false, acc) { prompt = it } }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val q = prompt.trim()
                if (q.isNotEmpty() && !busy) {
                    busy = true
                    msg = "Thinking..."
                    scope.launch {
                        val r = SoundAi.solve(q)
                        SoundFx.load(r.fx)
                        SoundFx.apply(s)
                        msg = r.note
                        busy = false
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = acc, contentColor = Color.Black)
        ) { Text(if (busy) "Thinking..." else "Make it \u2728", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        Text(msg, color = TextDim, fontSize = 13.sp)

        StudioSection("ROOM AND SPACE")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            val names = listOf("Off", "Small room", "Room", "Big room", "Hall", "Stadium", "Echo")
            names.forEachIndexed { i, n ->
                StudioChip(n, SoundFx.reverb == i, acc) {
                    SoundFx.reverb = i
                    SoundFx.apply(s)
                }
            }
        }
        StudioSlider("Muffle", SoundFx.muffle, 0f..1f, acc, (SoundFx.muffle * 100).toInt().toString() + "%",
            { SoundFx.muffle = it }, { SoundFx.apply(s) })
        StudioSlider("Bass", SoundFx.bass, 0f..1f, acc, (SoundFx.bass * 100).toInt().toString() + "%",
            { SoundFx.bass = it }, { SoundFx.apply(s) })

        StudioSection("SPEED AND PITCH")
        StudioSlider("Speed", SoundFx.speed, 0.6f..1.4f, acc, ((SoundFx.speed * 100).toInt() / 100f).toString() + "x",
            { SoundFx.speed = it }, { SoundFx.apply(s) })
        StudioSlider("Pitch", SoundFx.pitch, 0.7f..1.3f, acc, ((SoundFx.pitch * 100).toInt() / 100f).toString() + "x",
            { SoundFx.pitch = it }, { SoundFx.apply(s) })

        StudioSection("AMBIENT SOUNDS")
        StudioSlider("\uD83E\uDD97 Crickets", SoundFx.crickets, 0f..1f, acc, (SoundFx.crickets * 100).toInt().toString() + "%",
            { SoundFx.crickets = it }, { SoundFx.apply(s) })
        StudioSlider("\uD83C\uDF27\uFE0F Rain", SoundFx.rain, 0f..1f, acc, (SoundFx.rain * 100).toInt().toString() + "%",
            { SoundFx.rain = it }, { SoundFx.apply(s) })
        StudioSlider("\uD83D\uDD25 Fire", SoundFx.fire, 0f..1f, acc, (SoundFx.fire * 100).toInt().toString() + "%",
            { SoundFx.fire = it }, { SoundFx.apply(s) })
        StudioSlider("\uD83C\uDF0A Ocean", SoundFx.ocean, 0f..1f, acc, (SoundFx.ocean * 100).toInt().toString() + "%",
            { SoundFx.ocean = it }, { SoundFx.apply(s) })
        StudioSlider("\uD83C\uDF2C\uFE0F Wind", SoundFx.wind, 0f..1f, acc, (SoundFx.wind * 100).toInt().toString() + "%",
            { SoundFx.wind = it }, { SoundFx.apply(s) })

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    SoundFx.reset(s)
                    msg = "Back to normal."
                },
                colors = ButtonDefaults.buttonColors(containerColor = CardBg2, contentColor = Color.White)
            ) { Text("Reset") }
            Button(
                onClick = {
                    val nm = SoundFx.lastName.ifBlank { "My sound" }
                    StudioPrefs.addPreset(SoundFx.snapshot(nm))
                    msg = "Saved as \"" + nm + "\"."
                },
                colors = ButtonDefaults.buttonColors(containerColor = CardBg2, contentColor = Color.White)
            ) { Text("Save preset") }
        }
        if (StudioPrefs.presets.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                StudioPrefs.presets.forEach { p ->
                    StudioChip(p.name, false, acc) {
                        SoundFx.load(p)
                        SoundFx.apply(s)
                        msg = "Loaded \"" + p.name + "\"."
                    }
                    Text(
                        "\u2715",
                        color = TextDim,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(end = 14.dp).clickable { StudioPrefs.removePreset(p.name) }
                    )
                }
            }
        }

        StudioSection("LOOKS")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Star Struck on Home", color = Color.White, fontSize = 15.sp)
                Text("Night sky, pines and flying sparks", color = TextDim, fontSize = 12.sp)
            }
            Switch(checked = StudioPrefs.starOn, onCheckedChange = { StudioPrefs.starOn = it; StudioPrefs.save() })
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("String lights", color = Color.White, fontSize = 15.sp)
                Text("Glowing lights around the logo", color = TextDim, fontSize = 12.sp)
            }
            Switch(checked = StudioPrefs.lightsOn, onCheckedChange = { StudioPrefs.lightsOn = it; StudioPrefs.save() })
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            StringLightsFrame(96.dp) { AppLogo(80.dp) }
        }

        StudioSection("AI KEY")
        Text(
            "Paste your Anthropic API key so the AI can understand any sentence. It stays on this phone only. Without a key, simple offline keywords are used.",
            color = TextDim,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = StudioPrefs.apiKey,
            onValueChange = { StudioPrefs.apiKey = it; StudioPrefs.save() },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            placeholder = { Text("API key", color = TextDim) }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = StudioPrefs.model,
            onValueChange = { StudioPrefs.model = it; StudioPrefs.save() },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Model", color = TextDim) }
        )
        Spacer(Modifier.height(40.dp))
    }
}
