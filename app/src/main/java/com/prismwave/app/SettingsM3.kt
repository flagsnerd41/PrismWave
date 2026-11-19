package com.prismwave.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// ------------------------------------------------------------ morphing Material 3 shape badge
// A soft shape (cookie, clover, flower, scallop...) that slowly turns into the next one, forever.

private class BadgeShape(val lobes: Int, val amp: Float)

private val BADGE_SHAPES = listOf(
    BadgeShape(8, 0.07f),
    BadgeShape(4, 0.20f),
    BadgeShape(6, 0.15f),
    BadgeShape(12, 0.05f),
    BadgeShape(5, 0.17f)
)

@Composable
fun MorphBadge(diameter: Dp, color: Color, phase: Int = 0, content: @Composable () -> Unit = {}) {
    val total = BADGE_SHAPES.size
    val tr = rememberInfiniteTransition(label = "badge")
    val t by tr.animateFloat(
        0f, total.toFloat(),
        infiniteRepeatable(tween(total * 3200, easing = LinearEasing), RepeatMode.Restart),
        label = "badgeT"
    )
    val spin by tr.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "badgeSpin"
    )
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val still = Opts.lite
            val p = phase % total
            val tt = if (still) p.toFloat() else (t + p) % total
            val i = tt.toInt().coerceIn(0, total - 1)
            val j = (i + 1) % total
            val frac = tt - i
            // hold each shape for a while, then morph smoothly to the next one
            val k = ((frac - 0.55f) / 0.45f).coerceIn(0f, 1f)
            val f = k * k * (3f - 2f * k)
            val a = BADGE_SHAPES[i]
            val b = BADGE_SHAPES[j]
            val rot = if (still) 0f else Math.toRadians(spin.toDouble()).toFloat()
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val base = this.size.minDimension / 2f / 1.2f
            val path = Path()
            val steps = 120
            for (n in 0..steps) {
                val ang = n.toFloat() / steps * (2f * Math.PI.toFloat())
                val r1 = 1f + a.amp * cos(a.lobes * ang)
                val r2 = 1f + b.amp * cos(b.lobes * ang)
                val r = base * (r1 * (1f - f) + r2 * f)
                val x = cx + r * cos(ang + rot)
                val y = cy + r * sin(ang + rot)
                if (n == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color)
        }
        content()
    }
}

// ------------------------------------------------------------ header: one round "..." button with a dropdown menu

@Composable
fun HeaderMenuButton(s: AppState) {
    val ctx = LocalContext.current
    WhatsNew.init(ctx)
    var open by remember { mutableStateOf(false) }
    var showNews by remember { mutableStateOf(false) }
    LaunchedEffect(s.cookie) {
        if (s.signedIn && ProfilePic.url == null) ProfilePic.url = YtAccount.profilePhoto(s.cookie)
    }
    val accent = s.accent
    val menuBg = androidx.compose.ui.graphics.lerp(Night, accent, 0.14f)
    Box {
        RoundBtn(Icons.Rounded.MoreHoriz, 0.9f, { open = true }, 46.dp)
        if (!WhatsNew.seen) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5A6E))
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = menuBg,
            offset = DpOffset(0.dp, 8.dp)
        ) {
            // profile row
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        val url = ProfilePic.url
                        Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                            if (s.signedIn && url != null) {
                                val req = remember(url) { coil.request.ImageRequest.Builder(ctx).data(artUrl(url, 200)).crossfade(250).build() }
                                coil.compose.AsyncImage(
                                    model = req, contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AppLogo(34.dp)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            if (s.signedIn) (ProfilePic.name ?: "Your account") else "Sign in",
                            color = Color.White,
                            fontFamily = PrismFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(150.dp)
                        )
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                onClick = {
                    open = false
                    SettingsNav.start = "account"
                    s.screen = Screen.Settings
                }
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
            MenuRow(Icons.Rounded.NewReleases, "New releases", accent) {
                open = false
                WhatsNew.markSeen()
                SettingsNav.start = "changelog"
                s.screen = Screen.Settings
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
            MenuRow(Icons.Rounded.Info, "News From Developers", accent) {
                open = false
                showNews = true
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
            MenuRow(Icons.Rounded.History, "History", accent) {
                open = false
                s.openPlaylist = null
                s.libraryOpen = "History"
                s.screen = Screen.Library
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
            MenuRow(Icons.Rounded.BarChart, "Listening Stats", accent) {
                open = false
                SettingsNav.start = "stats"
                s.screen = Screen.Settings
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
            MenuRow(Icons.Rounded.Settings, "Settings", accent) {
                open = false
                s.screen = Screen.Settings
            }
        }
    }
    if (showNews) {
        val n = Updater.news
        AlertDialog(
            onDismissRequest = { showNews = false },
            confirmButton = { TextButton(onClick = { showNews = false }) { Text("OK") } },
            title = { Text(n?.title ?: "News From Developers") },
            text = { Text(n?.message ?: "Nothing new right now.") }
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 10.dp))
        },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f)) },
        onClick = onClick
    )
}
