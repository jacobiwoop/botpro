package com.example.botpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.botpro.data.model.Message
import com.example.botpro.data.model.MessageType
import com.example.botpro.data.model.ReplyQuote
import com.example.botpro.theme.TelegramColors

@Composable
fun MessageBubble(
    message: Message,
    onImageClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (message.type == MessageType.DATE_SEPARATOR) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x73111921))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = message.dateText ?: message.text ?: "",
                    color = Color(0xFFD6E2EE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    val isOutgoing = message.isOutgoing
    val bubbleColor = if (isOutgoing) TelegramColors.BubbleOut else TelegramColors.BubbleIn
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        if (message.type == MessageType.IMAGE && !message.imageUrl.isNullOrEmpty()) {
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .clickable { onImageClick(message.imageUrl) }
            ) {
                Box {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    )
                    if (message.text.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x99000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = message.time,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                                if (isOutgoing) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = if (message.isDoubleCheck) Icons.Default.DoneAll else Icons.Default.Done,
                                        contentDescription = "Statut",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!message.text.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = message.time,
                                color = if (isOutgoing) TelegramColors.CheckBubble else Color(0xFF8FA8C6),
                                fontSize = 11.sp
                            )
                            if (isOutgoing) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = if (message.isDoubleCheck) Icons.Default.DoneAll else Icons.Default.Done,
                                    contentDescription = "Statut",
                                    tint = TelegramColors.CheckBubble,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else if (message.type == MessageType.FILE && message.file != null) {
            // Carte fichier / photo
            Row(
                modifier = Modifier
                    .width(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bubbleColor)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = message.file.thumbnailUri,
                    contentDescription = message.file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C2938))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message.file.name,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = message.file.size,
                        color = Color(0xFF8FA8C6),
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.time,
                            color = TelegramColors.CheckBubble,
                            fontSize = 11.sp
                        )
                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = if (message.isDoubleCheck) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = "Statut",
                                tint = TelegramColors.CheckBubble,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Bulle texte standard
            Column(
                modifier = Modifier
                    .widthIn(min = 60.dp, max = 290.dp)
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                // Citation / Réponse si présente
                message.replyQuote?.let { quote ->
                    ReplyQuoteView(quote = quote)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Texte du message
                if (!message.text.isNullOrEmpty()) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Horodatage et coches
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.time,
                        color = if (isOutgoing) TelegramColors.CheckBubble else Color(0xFF6D7E8F),
                        fontSize = 11.sp
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = if (message.isDoubleCheck) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = "Statut",
                            tint = TelegramColors.CheckBubble,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyQuoteView(
    quote: ReplyQuote,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x1A000000))
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(36.dp)
                .background(Color(0xFF549CDA))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .padding(vertical = 2.dp)
                .weight(1f)
        ) {
            Text(
                text = quote.senderName,
                color = Color(0xFF549CDA),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = quote.text,
                color = Color(0xFF93A4B5),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
