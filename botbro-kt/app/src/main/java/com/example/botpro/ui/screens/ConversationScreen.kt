package com.example.botpro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.botpro.data.cache.ChatCache
import com.example.botpro.data.mock.MockData
import com.example.botpro.data.model.FileAttachment
import com.example.botpro.data.model.Message
import com.example.botpro.data.model.MessageType
import com.example.botpro.theme.TelegramColors
import com.example.botpro.ui.components.AttachmentBottomSheet
import com.example.botpro.ui.components.AttachmentType
import com.example.botpro.ui.components.ChatInputBar
import com.example.botpro.ui.components.ConversationHeader
import com.example.botpro.ui.components.ConversationSkeleton
import com.example.botpro.ui.components.MessageBubble
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConversationScreen(
    contactName: String = "Hernes Dav",
    contactInitials: String = "HE",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 1. Cache L1 synchrone à 0 ms
    val initialMemory = ChatCache.getMemoryCachedMessages(contactName)
    var messages by remember {
        mutableStateOf<List<Message>>(
            initialMemory ?: if (contactName == "Hernes Dav") MockData.conversationMessages else emptyList()
        )
    }
    var isLoading by remember { mutableStateOf(initialMemory == null && contactName != "Hernes Dav") }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    // 2. Chargement SWR : Disque L2 -> Skeleton fluide si vide -> Enregistrement cache
    LaunchedEffect(contactName) {
        if (initialMemory == null) {
            val diskMsgs = ChatCache.getDiskCachedMessages(context, contactName)
            if (diskMsgs.isNotEmpty()) {
                messages = diskMsgs
                isLoading = false
            } else {
                // Simuler un chargement réseau réaliste pour afficher le skeleton
                delay(600)
                val initialList = if (contactName == "BotFather") {
                    listOf(
                        Message(
                            id = "bf_welcome",
                            type = MessageType.TEXT,
                            isOutgoing = false,
                            text = "I can help you create and manage Telegram bots. If you're new to the Bot API, please see the manual.\n\nYou can control me by sending these commands:\n/newbot - create a new bot\n/mybots - edit your bots",
                            time = "12:00",
                            isRead = true
                        )
                    )
                } else if (contactName == "Hernes Dav") {
                    MockData.conversationMessages
                } else {
                    listOf(
                        Message(
                            id = "welcome_${System.currentTimeMillis()}",
                            type = MessageType.TEXT,
                            isOutgoing = false,
                            text = "👋 Bonjour ! Je suis **$contactName** sur BotPro.\n\nEnvoyez un message ou une commande pour démarrer !",
                            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                            isRead = true
                        )
                    )
                }
                messages = initialList
                isLoading = false
                ChatCache.saveCachedMessages(context, contactName, initialList)
            }
        }
    }

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
            val updated = messages + newMessage
            messages = updated
            scope.launch {
                ChatCache.saveCachedMessages(context, contactName, updated)
                listState.animateScrollToItem(0)
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
                file = FileAttachment(
                    name = "Document_Partagé.pdf",
                    size = "2.4 MB",
                    thumbnailUri = ""
                ),
                time = currentTime,
                isDoubleCheck = false
            )
            val updated = messages + newMessage
            messages = updated
            scope.launch {
                ChatCache.saveCachedMessages(context, contactName, updated)
                listState.animateScrollToItem(0)
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
        val updated = messages + newMessage
        messages = updated

        scope.launch {
            ChatCache.saveCachedMessages(context, contactName, updated)
            listState.animateScrollToItem(0)

            // Réponse automatique de bot simulée (interaction Telegram)
            val isBot = contactName.contains("bot", ignoreCase = true) ||
                        contactName.contains("aiko", ignoreCase = true) ||
                        contactName == "BotFather"

            if (isBot) {
                delay(700)
                val replyText = when {
                    contactName == "BotFather" && text.startsWith("/newbot") ->
                        "Alright, a new bot. How are we going to call it? Please choose a name for your bot."
                    contactName == "BotFather" ->
                        "BotFather: commande reçue. Tapez /newbot pour créer un bot ou /mybots pour administrer."
                    text.startsWith("/start") ->
                        "🤖 Bot démarré avec succès ! Je suis à votre service."
                    text.contains("vocal", ignoreCase = true) ->
                        "🎙️ Message vocal bien reçu et analysé."
                    else ->
                        "✅ Reçu 5/5 : « $text »"
                }

                val botReply = Message(
                    id = "bot_reply_${System.currentTimeMillis()}",
                    type = MessageType.TEXT,
                    isOutgoing = false,
                    text = replyText,
                    time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    isRead = true
                )
                val withReply = messages + botReply
                messages = withReply
                ChatCache.saveCachedMessages(context, contactName, withReply)
                listState.animateScrollToItem(0)
            }
        }
    }

    // Défilement initial vers le bas lors de l'ouverture
    LaunchedEffect(Unit) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    // Défilement automatique vers le bas à l'ouverture du clavier
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
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
                status = if (contactName.contains("bot", ignoreCase = true)) "bot" else "last seen recently",
                initials = contactInitials,
                onBack = onBack
            )
        }

        // Zone centrale : Skeleton UI animé ou Fil des messages réel
        if (isLoading && messages.isEmpty()) {
            ConversationSkeleton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = messages.asReversed(),
                    key = { it.id }
                ) { message ->
                    MessageBubble(
                        message = message,
                        onImageClick = { selectedImageUrl = it }
                    )
                }
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
                        val updated = messages + newMessage
                        messages = updated
                        scope.launch {
                            ChatCache.saveCachedMessages(context, contactName, updated)
                            listState.animateScrollToItem(0)
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
                        val updated = messages + newMessage
                        messages = updated
                        scope.launch {
                            ChatCache.saveCachedMessages(context, contactName, updated)
                            listState.animateScrollToItem(0)
                        }
                    }
                    AttachmentType.AUDIO -> {
                        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val newMessage = Message(
                            id = "msg_${System.currentTimeMillis()}",
                            type = MessageType.FILE,
                            isOutgoing = true,
                            file = FileAttachment(
                                name = "Audio_001.mp3",
                                size = "3.2 MB",
                                thumbnailUri = ""
                            ),
                            time = currentTime,
                            isDoubleCheck = false
                        )
                        val updated = messages + newMessage
                        messages = updated
                        scope.launch {
                            ChatCache.saveCachedMessages(context, contactName, updated)
                            listState.animateScrollToItem(0)
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
                        val updated = messages + newMessage
                        messages = updated
                        scope.launch {
                            ChatCache.saveCachedMessages(context, contactName, updated)
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            }
        )
    }

    // Modal d'affichage de photo en plein écran
    selectedImageUrl?.let { imageUrl ->
        Dialog(
            onDismissRequest = { selectedImageUrl = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Image agrandie",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )

                IconButton(
                    onClick = { selectedImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer l'image",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
