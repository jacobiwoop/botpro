package com.example.botpro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.botpro.theme.TelegramColors
import com.example.botpro.ui.screens.ChatListScreen
import com.example.botpro.ui.screens.ConversationScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(ChatListNavKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = Modifier
            .fillMaxSize()
            .background(TelegramColors.Base),
        entryProvider = entryProvider {
            entry<ChatListNavKey> {
                ChatListScreen(
                    onOpenChat = { name, initials ->
                        backStack.add(ConversationNavKey(name = name, initials = initials))
                    }
                )
            }
            entry<ConversationNavKey> { key ->
                ConversationScreen(
                    contactName = key.name,
                    contactInitials = key.initials,
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )
}
