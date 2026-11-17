import os, sys

p = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/Pages.kt")
t = open(p, encoding="utf-8").read()
added = []

if "fun TempoPitchDialog(" not in t:
    t += '''

// ------------------------------------------------------------ tempo and pitch: simple middle dialog (ArchiveTune style)

@Composable
fun TempoPitchDialog(s: AppState, onDismiss: () -> Unit) {
    fun semis(): Int = Math.round(12.0 * Math.log(s.pitch.toDouble()) / Math.log(2.0)).toInt()
    fun setTempo(v: Float) {
        s.tempo = Math.round(v.coerceIn(0.25f, 2f) * 20) / 20f
        s.applyTempoPitch()
    }
    fun setSemis(n: Int) {
        s.pitch = Math.pow(2.0, n.coerceIn(-12, 12) / 12.0).toFloat()
        s.applyTempoPitch()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF241F1F),
        shape = RoundedCornerShape(32.dp),
        title = { Text("Tempo and Pitch", color = Color.White, fontSize = 28.sp) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = s.accent, fontSize = 17.sp) } },
        dismissButton = {
            TextButton(onClick = {
                s.tempo = 1f
                s.pitch = 1f
                s.applyTempoPitch()
            }) { Text("Reset", color = s.accent, fontSize = 17.sp) }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speed, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(30.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StepIcon(Icons.Rounded.Remove) { setTempo(s.tempo - 0.05f) }
                    }
                    Text(
                        "x" + "%.2f".format(s.tempo),
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.3f)
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StepIcon(Icons.Rounded.Add) { setTempo(s.tempo + 0.05f) }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(30.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StepIcon(Icons.Rounded.Remove) { setSemis(semis() - 1) }
                    }
                    Text(
                        (if (semis() > 0) "+" else "") + semis(),
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.3f)
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StepIcon(Icons.Rounded.Add) { setSemis(semis() + 1) }
                    }
                }
            }
        }
    )
}
'''
    added.append("TempoPitchDialog")

if "fun FeaturedCard(" not in t:
    t += '''

// ------------------------------------------------------------ Home: big "Keep listening" card

@Composable
fun FeaturedCard(song: Song, from: List<Song>, s: AppState, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    val art = rememberArtColor(song)
    val fade = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.40f to Color.Transparent,
            0.75f to Color.Black,
            1f to Color.Black
        )
    }
    Box(
        modifier
            .height(330.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
    ) {
        Cover(song, Modifier.fillMaxSize(), 0.dp, 90.sp, quality = 900)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = fade, blendMode = BlendMode.DstIn)
                }
        ) { Cover(song, Modifier.fillMaxSize().blur(24.dp), 0.dp, 90.sp, quality = 400) }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.40f to Color.Transparent,
                    1f to lerp(Color.Black, art, 0.25f).copy(alpha = 0.92f)
                )
            )
        )
        Row(
            Modifier
                .padding(18.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(s.accent))
            Spacer(Modifier.width(8.dp))
            Text("KEEP LISTENING", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
            Text(
                song.title,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(song.artist, color = Color.White.copy(alpha = 0.75f), fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier
                        .bounceClick { s.play(song, from) }
                        .clip(RoundedCornerShape(50))
                        .background(s.accent)
                        .padding(horizontal = 24.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Play", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
                Row(
                    Modifier
                        .bounceClick {
                            s.shuffle = true
                            s.play(from.random(), from)
                        }
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
'''
    added.append("FeaturedCard")

open(p, "w", encoding="utf-8").write(t)
print("restored: " + (", ".join(added) if added else "nothing, both already there"))
