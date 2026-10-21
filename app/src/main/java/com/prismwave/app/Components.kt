package com.prismwave.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun fmt(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

@Composable
fun PLogo(size: Dp, color: Color = Gold) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text("P", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.55f).sp)
    }
}

@Composable
fun Cover(song: Song, modifier: Modifier = Modifier, radius: Dp = 14.dp, letterSize: TextUnit = 22.sp) {
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(listOf(song.color, Night))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            song.title.take(1),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = letterSize,
            fontWeight = FontWeight.Bold
        )
        if (song.thumb != null) {
            AsyncImage(
                model = song.thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun Glass(
    alpha: Float,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        Modifier
            .clip(shape)
            .then(modifier)
            .background(Night)
            .background(Surface2.copy(alpha = alpha))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape),
        content = content
    )
}

@Composable
fun RoundBtn(
    icon: ImageVector,
    alpha: Float,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    tint: Color = Color.White
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Surface2.copy(alpha = alpha))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)
    )
}

@Composable
fun SongRow(song: Song, s: AppState, from: List<Song> = listOf(song)) {
    val isCurrent = s.current?.id == song.id
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { s.play(song, from) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(song, Modifier.size(52.dp), 12.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) s.accent else Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist + "  •  " + fmt(song.seconds),
                color = TextDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val liked = s.isLiked(song)
        Icon(
            imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = null,
            tint = if (liked) s.accent else TextDim,
            modifier = Modifier.size(24.dp).clickable { s.toggleLike(song) }
        )
    }
}
