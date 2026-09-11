package com.example.botpro

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.botpro.theme.TelegramColors
import com.example.botpro.ui.screens.AuthScreen
import com.example.botpro.ui.screens.BotFatherScreen
import com.example.botpro.ui.screens.ChatListScreen
import com.example.botpro.ui.screens.ConversationScreen

@Composable
fun MainNavigation() {
    val backStack = remember { mutableStateListOf<Any>(ChatListNavKey) }
    val current = backStack.lastOrNull() ?: ChatListNavKey

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    AnimatedContent(
        targetState = current,
        transitionSpec = {
            if (targetState is ConversationNavKey || targetState is BotFatherNavKey || targetState is AuthNavKey) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width / 4 } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width / 4 } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(TelegramColors.Base),
        label = "ScreenTransition"
    ) { key ->
        when (key) {
            is ConversationNavKey -> {
                ConversationScreen(
                    contactName = key.name,
                    contactInitials = key.initials,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                )
            }
            is BotFatherNavKey -> {
                BotFatherScreen(
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    },
                    onOpenChatWithBot = { botName, botInitials ->
                        backStack.add(ConversationNavKey(name = botName, initials = botInitials))
                    }
                )
            }
            is AuthNavKey -> {
                AuthScreen(
                    onAuthSuccess = {
                        backStack.clear()
                        backStack.add(ChatListNavKey)
                    },
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    }
                )
            }
            else -> {
                ChatListScreen(
                    onOpenChat = { name, initials ->
                        backStack.add(ConversationNavKey(name = name, initials = initials))
                    },
                    onOpenBotFather = {
                        backStack.add(BotFatherNavKey())
                    },
                    onOpenAuth = {
                        backStack.add(AuthNavKey)
                    }
                )
            }
        }
    }
}
