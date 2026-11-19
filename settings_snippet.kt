// ------------------------------------------------------------ new Settings main page (profile card + Essentials tiles)

@Composable
private fun PfProfileCard(s: AppState, onManage: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(s.cookie) {
        if (s.signedIn && ProfilePic.url == null) ProfilePic.url = YtAccount.profilePhoto(s.cookie)
    }
    val accent = s.accent
    val url = ProfilePic.url
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(androidx.compose.ui.graphics.lerp(CardBg2, accent, 0.30f))
    ) {
        Box(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 14.dp)) {
            MorphBadge(120.dp, accent.copy(alpha = 0.16f), 2)
        }
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                MorphBadge(104.dp, accent.copy(alpha = 0.55f), 0) {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        if (s.signedIn && url != null) {
                            val req = remember(url) { coil.request.ImageRequest.Builder(ctx).data(artUrl(url, 300)).crossfade(300).build() }
                            coil.compose.AsyncImage(
                                model = req, contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AppLogo(52.dp)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .softClick { onManage() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manage account", color = Night, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Night, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (s.signedIn) (ProfilePic.name ?: "Your account") else "PrismWave",
                color = Color.White,
                fontFamily = PrismFont,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (s.signedIn) "YouTube Music" else "Not signed in",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun PfTile(
    title: String,
    sub: String,
    icon: ImageVector,
    accent: Color,
    neutral: Boolean,
    phase: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg = if (neutral) CardBg2 else androidx.compose.ui.graphics.lerp(CardBg, accent, 0.30f)
    val badge = if (neutral) Color.White.copy(alpha = 0.88f) else accent
    Column(
        modifier
            .height(190.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(bg)
            .softClick { onClick() }
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        MorphBadge(64.dp, badge, phase) {
            Icon(icon, contentDescription = null, tint = Night, modifier = Modifier.size(28.dp))
        }
        Column {
            Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, color = TextDim, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PfEssentials(s: AppState, onOpen: (String) -> Unit) {
    val a = s.accent
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PfTile("Appearance", "Themes, layout, and visual styles", Icons.Rounded.Palette, a, false, 0, Modifier.weight(1f)) { onOpen("appearance") }
            PfTile("Playback", "Audio behavior, crossfade, and more", Icons.Rounded.MusicNote, a, false, 1, Modifier.weight(1f)) { onOpen("player") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PfTile("Lyrics", "Lyrics size and alignment", Icons.Rounded.Subject, a, false, 2, Modifier.weight(1f)) { onOpen("lyrics") }
            PfTile("Listening Stats", "Your top songs, artists, and play time", Icons.Rounded.BarChart, a, true, 3, Modifier.weight(1f)) { onOpen("stats") }
        }
    }
}

@Composable
private fun PfLyricsPage(s: AppState) {
    val a = s.accent
    LyricsPrefs.init(LocalContext.current.applicationContext)
    PfLabel("Lyrics", a)
    PfGroup {
        PfChoice(
            Icons.Rounded.Subject, "Text size", listOf("Small", "Medium", "Large"), LyricsPrefs.size, a,
            "How big the lines are on the lyrics page"
        ) {
            LyricsPrefs.size = it
            LyricsPrefs.save()
        }
        PfSwitch(Icons.Rounded.Subject, "Center lyrics", "Center the lines instead of left aligned", LyricsPrefs.center, a) {
            LyricsPrefs.center = it
            LyricsPrefs.save()
        }
    }
}

// ------------------------------------------------------------ Listening Stats with time ranges

@Composable
private fun PfStatCard(icon: ImageVector, value: String, label: String, accent: Color, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(CardBg)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphBadge(52.dp, accent, label.length) { Icon(icon, contentDescription = null, tint = Night, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = TextDim, fontSize = 13.sp, maxLines = 2)
        }
    }
}

@Composable
private fun PfStatsPage2(s: AppState) {
    val a = s.accent
    val ranges = listOf("1 week" to 7, "1 month" to 30, "3 months" to 90, "1 year" to 365, "All time" to 0)
    var sel by remember { mutableStateOf(0) }
    val days = ranges[sel].second
    val secs = s.statSeconds(days)
    val counts = s.statCounts(days)
    val played = counts.entries.mapNotNull { e -> s.songOf(e.key)?.let { it to e.value } }
    val topSongs = played.sortedByDescending { it.second }.take(5)
    val artistCounts = played.groupBy({ it.first.artist }, { it.second }).map { it.key to it.value.sum() }.sortedByDescending { it.second }
    val totalPlays = if (days == 0) s.playCount else counts.values.sum()
    val bigNumber = if (secs >= 3600) (secs / 3600).toString() + "h " + ((secs / 60) % 60).toString() + "m" else (secs / 60).toString()
    val bigUnit = if (secs >= 3600) "hours and minutes" else "minutes"
    val clock = (secs / 60).toString() + ":" + (secs % 60).toString().padStart(2, '0')
    PfLabel("Time range", a)
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ranges.forEachIndexed { i, r ->
            val on = i == sel
            Text(
                r.first,
                color = if (on) Night else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) a else CardBg)
                    .softClick { sel = i }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(androidx.compose.ui.graphics.lerp(CardBg2, a, 0.32f))
    ) {
        Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)) { MorphBadge(150.dp, a.copy(alpha = 0.18f), 1) }
        Column(Modifier.padding(24.dp)) {
            Text(
                ranges[sel].first,
                color = Night,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(a).padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Total Time Listened", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(bigNumber, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Bold, fontFamily = PrismFont)
            Text(bigUnit + "  ·  " + clock, color = Color.White.copy(alpha = 0.75f), fontSize = 16.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PfStatCard(Icons.Rounded.PlayArrow, totalPlays.toString(), "Total Plays", a, Modifier.weight(1f))
        PfStatCard(Icons.Rounded.MusicNote, counts.size.toString(), "Unique Songs", a, Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PfStatCard(Icons.Rounded.AccountCircle, artistCounts.size.toString(), "Unique Artists", a, Modifier.weight(1f))
        PfStatCard(Icons.Rounded.Speed, if (totalPlays > 0) (secs / totalPlays / 60).toString() + " min" else "-", "Average per song", a, Modifier.weight(1f))
    }
    if (topSongs.isNotEmpty()) {
        PfLabel("Top songs", a)
        PfGroup {
            topSongs.forEachIndexed { i, p ->
                PfRow(
                    Icons.Rounded.MusicNote, (i + 1).toString() + ". " + p.first.title, p.first.artist, a, null,
                    trailing = { Text(p.second.toString() + "x", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium) }
                )
            }
        }
    }
    if (artistCounts.isNotEmpty()) {
        PfLabel("Top artists", a)
        PfGroup {
            artistCounts.take(5).forEachIndexed { i, p ->
                PfRow(
                    Icons.Rounded.AccountCircle, (i + 1).toString() + ". " + p.first, null, a, null,
                    trailing = { Text(p.second.toString() + "x", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium) }
                )
            }
        }
    }
    PfNote("Time ranges count from this update on. All time includes everything before. Counted on this phone only.")
}

