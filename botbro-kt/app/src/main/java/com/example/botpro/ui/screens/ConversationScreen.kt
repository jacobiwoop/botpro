package com.example.botpro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.botpro.data.mock.MockData
import com.example.botpro.data.model.Message
import com.example.botpro.data.model.MessageType
import com.example.botpro.theme.TelegramColors
import com.example.botpro.ui.components.AttachmentBottomSheet
import com.example.botpro.ui.components.AttachmentType
import com.example.botpro.ui.components.ChatInputBar
import com.example.botpro.ui.components.ConversationHeader
import com.example.botpro.ui.components.MessageBubble
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    contactName: String = "Hernes Dav",
    contactInitials: String = "HE",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var messages by remember { mutableStateOf(MockData.conversationMessages) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val newMessage = Message(
                id = "msg_${System.currentTimeMillis()}",
                type = MessageType.IMAGE,
                isOutgoing = true,
                imageUrl = it.toString(),
                text = "Photo partagée 📸",
                time = currentTime,
                isDoubleCheck = false
            )
            messages = messages + newMessage
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val newMessage = Message(
                id = "msg_${System.currentTimeMillis()}",
                type = MessageType.FILE,
                isOutgoing = true,
                fileName = "Document_Partagé.pdf",
                fileSize = "2.4 MB",
                time = currentTime,
                isDoubleCheck = false
            )
            messages = messages + newMessage
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

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
    ) {
        // En-tête fixe avec couleur dédiée sous la barre d'état
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
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                MessageBubble(
                    message = message,
                    onImageClick = { selectedImageUrl = it }
                )
            }
        }

        // Barre de saisie inférieure avec insets dynamiques
        ChatInputBar(
            onSendMessage = onSendMessage,
            onAttachmentClick = {
                keyboardController?.hide()
                showAttachmentSheet = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        )
    }

    // Modal Bottom Sheet pour les pièces jointes
    if (showAttachmentSheet) {
        AttachmentBottomSheet(
            sheetState = sheetState,
            onDismiss = { showAttachmentSheet = false },
            onSelectAttachment = { type ->
                showAttachmentSheet = false
                when (type) {
                    AttachmentType.GALLERY -> {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    AttachmentType.CAMERA -> {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    AttachmentType.DOCUMENT -> {
                        docPickerLauncher.launch("*/*")
                    }
                    AttachmentType.CONTACT -> {
                        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val newMessage = Message(
                            id = "msg_${System.currentTimeMillis()}",
                            type = MessageType.TEXT,
                            isOutgoing = true,
                            text = "👤 Contact partagé :\nMartha Craig\n📱 +81 3-1234-5678",
                            time = currentTime,
                            isDoubleCheck = false
                        )
                        messages = messages + newMessage
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                    AttachmentType.LOCATION -> {
                        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val newMessage = Message(
                            id = "msg_${System.currentTimeMillis()}",
                            type = MessageType.TEXT,
                            isOutgoing = true,
                            text = "📍 Position partagée :\nTour de Tokyo, Minato City, Tokyo 105-0011",
                            time = currentTime,
                            isDoubleCheck = false
                        )
                        messages = messages + newMessage
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                    AttachmentType.AUDIO -> {
                        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val newMessage = Message(
                            id = "msg_${System.currentTimeMillis()}",
                            type = MessageType.FILE,
                            isOutgoing = true,
                            fileName = "Audio_001.mp3",
                            fileSize = "3.2 MB",
                            time = currentTime,
                            isDoubleCheck = false
                        )
                        messages = messages + newMessage
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                    AttachmentType.POLL -> {
                        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val newMessage = Message(
                            id = "msg_${System.currentTimeMillis()}",
                            type = MessageType.TEXT,
                            isOutgoing = true,
                            text = "📊 Sondage :\nQuand se retrouve-t-on pour dîner ?\n▫️ Ce soir à 20h\n▫️ Demain midi\n▫️ Ce week-end",
                            time = currentTime,
                            isDoubleCheck = false
                        )
                        messages = messages + newMessage
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
            }
        )
    }

    // Dialogue d'aperçu d'image en plein écran
    if (selectedImageUrl != null) {
        Dialog(
            onDismissRequest = { selectedImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Plein écran",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { selectedImageUrl = null },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
