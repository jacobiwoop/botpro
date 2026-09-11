package com.example.botpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.botpro.data.mock.MockData
import com.example.botpro.theme.TelegramColors
import com.example.botpro.ui.components.ChatItemRow
import com.example.botpro.ui.components.DrawerContent
import com.example.botpro.ui.components.TelegramFab
import com.example.botpro.ui.components.TelegramHeader
import kotlinx.coroutines.launch

@Composable
fun ChatListScreen(
    onOpenChat: (name: String, initials: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val chatList = MockData.chatListData

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onClose = {
                    scope.launch { drawerState.close() }
                }
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
                        .background(TelegramColors.Header)
                        .statusBarsPadding()
                ) {
                    TelegramHeader(
                        onMenuClick = {
                            scope.launch { drawerState.open() }
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
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 24.dp)
            )
        }
    }
}
