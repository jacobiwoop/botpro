package com.example.botpro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.botpro.data.api.ApiClient
import com.example.botpro.theme.TelegramColors

@Composable
fun DrawerContent(
    onClose: () -> Unit,
    onOpenBotFather: () -> Unit = {},
    onOpenAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAccounts by remember { mutableStateOf(true) }
    var isNightMode by remember { mutableStateOf(true) }

    val currentUser = ApiClient.currentUser
    val displayName = currentUser?.let { "${it.firstName} ${it.lastName ?: ""}".trim() } ?: "darren lee"
    val initials = if (displayName.isNotBlank()) {
        displayName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
    } else "DL"
    val displaySubtitle = currentUser?.username?.let { "@$it" } ?: currentUser?.email ?: "+44 7354 224381"

    ModalDrawerSheet(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp),
        drawerContainerColor = TelegramColors.Base,
        drawerShape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // En-tête profil
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TelegramColors.Header)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8261E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAccounts = !showAccounts },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displaySubtitle,
                            color = TelegramColors.TextMuted,
                            fontSize = 13.sp
                        )
                    }

                    Icon(
                        imageVector = if (showAccounts) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Comptes",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Section comptes
            AnimatedVisibility(visible = showAccounts) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TelegramColors.Header)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8261E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Actif",
                            tint = TelegramColors.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onClose()
                                onOpenAuth()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x338596A7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Changer de compte",
                                tint = Color(0xFF8596A7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Switch Account",
                            color = Color(0xFF8596A7),
                            fontSize = 15.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0x33101921), thickness = 1.dp)

            // Rubriques menu
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Entrée BotFather (NEW)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onClose()
                            onOpenBotFather()
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "BotFather",
                        tint = Color(0xFF52A6E6),
                        modifier = Modifier.size(23.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "BotFather",
                        color = Color(0xFF52A6E6),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF52A6E6))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NEW",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                DrawerMenuItem(
                    icon = Icons.Default.Person,
                    title = "Contacts",
                    onClick = onClose
                )
                DrawerMenuItem(
                    icon = Icons.Default.Call,
                    title = "Calls",
                    onClick = onClose
                )
                DrawerMenuItem(
                    icon = Icons.Default.Bookmark,
                    title = "Saved Messages",
                    onClick = onClose
                )
                DrawerMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    onClick = onClose
                )
                DrawerMenuItem(
                    icon = Icons.Default.PersonAdd,
                    title = "Invite Friends",
                    onClick = onClose
                )
                DrawerMenuItem(
                    icon = Icons.Default.Help,
                    title = "Telegram Features",
                    onClick = onClose
                )
            }

            HorizontalDivider(color = Color(0x33101921), thickness = 1.dp)

            // Switch Mode Nuit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "Night Mode",
                        tint = Color(0xFF8596A7),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Night Mode",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = isNightMode,
                    onCheckedChange = { isNightMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TelegramColors.Fab,
                        uncheckedThumbColor = Color(0xFFF4F3F4),
                        uncheckedTrackColor = Color(0xFF3A4B5D)
                    )
                )
            }

            HorizontalDivider(color = Color(0x33101921), thickness = 1.dp)

            // Bouton Déconnexion (Log Out)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onClose()
                        onOpenAuth()
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Log Out",
                    tint = Color(0xFFE17076),
                    modifier = Modifier.size(23.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = "Log Out",
                    color = Color(0xFFE17076),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF8596A7),
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
