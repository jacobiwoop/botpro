package com.example.botpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.botpro.data.mock.MockData
import com.example.botpro.data.model.Message
import com.example.botpro.data.model.MessageType
import com.example.botpro.theme.TelegramColors
import com.example.botpro.ui.components.ChatInputBar
import com.example.botpro.ui.components.ConversationHeader
import com.example.botpro.ui.components.MessageBubble
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationScreen(
    contactName: String = "Hernes Dav",
    contactInitials: String = "HE",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var messages by remember { mutableStateOf(MockData.conversationMessages) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val onSendMessage: (String) -> Unit = { text ->
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val newMessage = Message(
            id = "msg_${System.currentTimeMillis()}",
            type = MessageType.TEXT,
            isOutgoing = true,
            text = text,
            time = currentTime,
            isDoubleCheck = false
        )
        messages = messages + newMessage
        scope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Défilement initial vers le bas lors de l'ouverture
    LaunchedEffect(Unit) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TelegramColors.ChatBackground)
            .imePadding()
    ) {
        // En-tête avec couleur dédiée sous la barre d'état
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TelegramColors.Header)
                .statusBarsPadding()
        ) {
            ConversationHeader(
                name = contactName,
                status = "last seen recently",
                initials = contactInitials,
                onBack = onBack
            )
        }

        // Fil des messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                MessageBubble(message = message)
            }
        }

        // Barre de saisie inférieure
        ChatInputBar(onSendMessage = onSendMessage)
    }
}
