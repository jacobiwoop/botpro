package com.example.botpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.botpro.data.api.ApiClient
import com.example.botpro.data.cache.ChatCache
import com.example.botpro.data.mock.MockData
import com.example.botpro.data.model.AvatarType
import com.example.botpro.data.model.ChatItem
import com.example.botpro.theme.TelegramColors
import com.example.botpro.ui.components.BotSearchModal
import com.example.botpro.ui.components.ChatItemRow
import com.example.botpro.ui.components.DrawerContent
import com.example.botpro.ui.components.TelegramFab
import com.example.botpro.ui.components.TelegramHeader
import kotlinx.coroutines.launch

@Composable
fun ChatListScreen(
    onOpenChat: (name: String, initials: String) -> Unit,
    onOpenBotFather: () -> Unit = {},
    onOpenAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSearchModal by remember { mutableStateOf(false) }

    // 1. Initialisation instantanée 0 ms via Cache L1 (RAM) ou Mock initial
    val memoryChats = ChatCache.getMemoryCachedChats()
    var chatList by remember {
        mutableStateOf(memoryChats ?: MockData.chatListData)
    }

    // 2. Stratégie SWR : Lecture disque L2 puis revalidation réseau
    LaunchedEffect(Unit) {
        if (memoryChats == null) {
            val diskChats = ChatCache.getDiskCachedChats(context)
            if (diskChats.isNotEmpty()) {
                chatList = diskChats
            }
        }

        // Revalidation en tâche de fond avec les bots disponibles
        try {
            val bots = ApiClient.fetchBots()
            if (bots.isNotEmpty()) {
                val botChats = bots.map { bot ->
                    ChatItem(
                        id = "bot_${bot.id}",
                        name = bot.firstName,
                        avatarType = AvatarType.INITIALS,
                        avatarBg = "#5288C1",
                        initials = bot.firstName.take(2).uppercase(),
                        lastMessage = bot.about ?: "Tapez /start pour démarrer",
                        time = "12:00",
                        unreadCount = 0,
                        isRead = true
                    )
                }

                // Fusionner avec les conversations existantes pour éviter les doublons
                val merged = mutableListOf<ChatItem>()
                merged.addAll(botChats)
                MockData.chatListData.forEach { defaultChat ->
                    if (merged.none { it.name.equals(defaultChat.name, ignoreCase = true) }) {
                        merged.add(defaultChat)
                    }
                }

                chatList = merged
                ChatCache.saveCachedChats(context, merged)
            }
        } catch (e: Exception) {
            // Conserver l'état actuel en cas d'erreur
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onClose = {
                    scope.launch { drawerState.close() }
                },
                onOpenBotFather = onOpenBotFather,
                onOpenAuth = onOpenAuth
            )
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(TelegramColors.Base)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Barre d'en-tête avec couleur dédiée sous la barre d'état
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TelegramColors.Header)
                        .statusBarsPadding()
                ) {
                    TelegramHeader(
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        },
                        onSearchClick = {
                            showSearchModal = true
                        }
                    )
                }

                // Liste déroulante des conversations
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TelegramColors.Base),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    itemsIndexed(
                        items = chatList,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        ChatItemRow(
                            item = item,
                            isLast = index == chatList.lastIndex,
                            onClick = {
                                val initials = item.initials ?: item.name.take(2).uppercase()
                                onOpenChat(item.name, initials)
                            }
                        )
                    }
                }
            }

            // Bouton flottant de nouveau message (FAB)
            TelegramFab(
                onClick = {
                    showSearchModal = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 24.dp)
            )

            // Modal de recherche de bots instantanée
            BotSearchModal(
                visible = showSearchModal,
                onClose = { showSearchModal = false },
                onSelectBot = { _, botName, _ ->
                    showSearchModal = false
                    val initials = botName.take(2).uppercase()
                    onOpenChat(botName, initials)
                }
            )
        }
    }
}
