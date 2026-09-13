package com.example.botpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.botpro.data.api.ApiClient
import com.example.botpro.data.models.ApiBot
import com.example.botpro.theme.TelegramColors
import kotlinx.coroutines.launch

@Composable
fun BotSearchModal(
    visible: Boolean,
    onClose: () -> Unit,
    onSelectBot: (chatId: String, botName: String, botUsername: String) -> Unit
) {
    if (!visible) return

    var query by remember { mutableStateOf("") }
    var bots by remember { mutableStateOf<List<ApiBot>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Charger les bots au montage et filtrer selon la recherche
    LaunchedEffect(visible, query) {
        loading = true
        val allBots = ApiClient.fetchBots()
        val filtered = if (query.isBlank()) {
            allBots
        } else {
            val q = query.trim().lowercase().removePrefix("@")
            allBots.filter { bot ->
                bot.username.lowercase().contains(q) ||
                bot.firstName.lowercase().contains(q) ||
                (bot.about?.lowercase()?.contains(q) == true)
            }
        }
        bots = filtered
        loading = false
    }

    // Demande de focus sur le champ de recherche
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignorer si le focus n'est pas prêt
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TelegramColors.Base)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Barre d'en-tête de recherche
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(TelegramColors.Header)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color(0xFF8596A7)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Rechercher un bot (@aikobot, BotFather)...",
                                color = Color(0xFF8596A7),
                                fontSize = 16.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(TelegramColors.Accent),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Effacer",
                                tint = Color(0xFF8596A7)
                            )
                        }
                    }
                }

                // Titre de section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131E2B))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "BOTS DISPONIBLES DANS BOTPRO",
                        color = TelegramColors.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = TelegramColors.Accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (bots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (query.isBlank()) "Aucun bot disponible pour le moment"
                                   else "Aucun bot trouvé pour « $query »",
                            color = Color(0xFF8E9DAE),
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TelegramColors.Base)
                    ) {
                        items(bots, key = { it.id }) { bot ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onClose()
                                        scope.launch {
                                            val chatId = com.example.botpro.data.supabase.SupabaseService.getOrCreateBotChat(
                                                bot.id,
                                                bot.firstName,
                                                bot.username
                                            )
                                            onSelectBot(chatId.toString(), bot.firstName, bot.username)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar robot
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF5288C1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = "Bot",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Informations
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = bot.firstName,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2B5278))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "BOT",
                                                color = Color(0xFF64B5F6),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "@${bot.username}",
                                        color = TelegramColors.Accent,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    if (!bot.about.isNullOrBlank()) {
                                        Text(
                                            text = bot.about,
                                            color = Color(0xFF8E9DAE),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 3.dp)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF8596A7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            HorizontalDivider(
                                color = Color(0xFF242F3D),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 78.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
