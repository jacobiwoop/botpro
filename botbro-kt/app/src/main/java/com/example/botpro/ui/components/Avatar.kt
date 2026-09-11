package com.example.botpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.botpro.data.model.AvatarType
import com.example.botpro.data.model.ChatItem

fun parseColorSafe(hexColor: String?, defaultColor: Color): Color {
    if (hexColor.isNullOrEmpty()) return defaultColor
    return try {
        val cleanHex = hexColor.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        defaultColor
    }
}

@Composable
fun Avatar(
    item: ChatItem,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (item.avatarType) {
            AvatarType.IMAGE -> {
                if (!item.avatarUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.avatarUri,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xFF334155))
                    )
                }
            }
            AvatarType.TELEGRAM -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF2693D9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Telegram",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.5f)
                    )
                }
            }
            AvatarType.INITIALS -> {
                val bg = parseColorSafe(item.avatarBg, Color(0xFF8261E6))
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.initials ?: item.name.take(2).uppercase(),
                        color = Color.White,
                        fontSize = (size.value * 0.35f).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            AvatarType.GETPAY -> {
                val bg = parseColorSafe(item.avatarBg, Color(0xFF0095D9))
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "GETPAY",
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            AvatarType.DOT -> {
                val bg = parseColorSafe(item.avatarBg, Color(0xFF4E97CF))
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
            AvatarType.SAVED -> {
                val bg = parseColorSafe(item.avatarBg, Color(0xFF4EA4E6))
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = "Saved Messages",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleInitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    backgroundColor: Color = Color(0xFF8261E6)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
