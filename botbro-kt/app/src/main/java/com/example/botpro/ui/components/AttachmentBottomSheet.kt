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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.botpro.theme.TelegramColors

enum class AttachmentType {
    GALLERY,
    DOCUMENT,
    CAMERA,
    LOCATION,
    CONTACT,
    AUDIO,
    POLL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectAttachment: (AttachmentType) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TelegramColors.DrawerBg,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Partager du contenu",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 18.dp, start = 4.dp)
            )

            // Première rangée
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AttachmentItem(
                    icon = Icons.Default.Image,
                    label = "Galerie",
                    color = Color(0xFF9C27B0),
                    onClick = {
                        onDismiss()
                        onSelectAttachment(AttachmentType.GALLERY)
                    }
                )
                AttachmentItem(
                    icon = Icons.Default.Description,
                    label = "Fichier",
                    color = Color(0xFF1E88E5),
                    onClick = {
                        onDismiss()
                        onSelectAttachment(AttachmentType.DOCUMENT)
                    }
                )
                AttachmentItem(
                    icon = Icons.Default.CameraAlt,
                    label = "Caméra",
                    color = Color(0xFFE91E63),
                    onClick = {
                        onDismiss()
                        onSelectAttachment(AttachmentType.CAMERA)
                    }
                )
                AttachmentItem(
                    icon = Icons.Default.LocationOn,
                    label = "Lieu",
                    color = Color(0xFF43A047),
                    onClick = {
                        onDismiss()
                        onSelectAttachment(AttachmentType.LOCATION)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Deuxième rangée
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AttachmentItem(
                    icon = Icons.Default.Person,
                    label = "Contact",
                    color = Color(0xFF00ACC1),
                    onClick = {
                        onDismiss()
                        onSelectAttachment(AttachmentType.CONTACT)
                    }
                )
                AttachmentItem(
                    icon = Icons.Default.Headphones,
                    label = "Musique",
                    color = Color(0xFFFB8C00),
                    onClick = {
                        onDismiss()
                        onSelectAttachment(AttachmentType.AUDIO)
                    }
                )
                AttachmentItem(
                    icon = Icons.Default.Poll,
                    label = "Sondage",
                    color = Color(0xFFFFB300),
                    onClick = {
                        onDismiss()
                        onSelectAttachment(AttachmentType.POLL)
                    }
                )
                Spacer(modifier = Modifier.size(68.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AttachmentItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .size(width = 68.dp, height = 76.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color(0xFFD6E2EE),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
