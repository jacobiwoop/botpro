package com.example.botpro.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.botpro.data.api.ApiClient
import com.example.botpro.data.models.ApiBot
import com.example.botpro.data.models.ApiWebhookInfo
import com.example.botpro.theme.TelegramColors
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class BotFatherMode {
    LIST, CREATE, DETAILS
}

private val AVATAR_COLORS = listOf(
    Color(0xFF52A6E6),
    Color(0xFFE17076),
    Color(0xFF6ECC51),
    Color(0xFFEFA141),
    Color(0xFFA695E7),
    Color(0xFF2AABEE)
)

private fun getAvatarColor(name: String): Color {
    val hash = abs(name.hashCode())
    return AVATAR_COLORS[hash % AVATAR_COLORS.size]
}

@Composable
fun BotFatherScreen(
    onBack: () -> Unit,
    onOpenChatWithBot: (botName: String, botInitials: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var viewMode by remember { mutableStateOf(BotFatherMode.LIST) }
    var bots by remember { mutableStateOf<List<ApiBot>>(emptyList()) }
    var selectedBot by remember { mutableStateOf<ApiBot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Formulaire de création
    var newBotName by remember { mutableStateOf("") }
    var newBotAbout by remember { mutableStateOf("") }
    var newBotUsername by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    // État Details
    var showToken by remember { mutableStateOf(false) }
    var webhookInfo by remember { mutableStateOf<ApiWebhookInfo?>(null) }
    var loadingWebhook by remember { mutableStateOf(false) }
    var showWebhookDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRevokeConfirmDialog by remember { mutableStateOf(false) }

    // Webhook form
    var webhookUrlInput by remember { mutableStateOf("") }
    var webhookSecretInput by remember { mutableStateOf("") }
    var dropPendingUpdates by remember { mutableStateOf(false) }
    var savingWebhook by remember { mutableStateOf(false) }

    fun refreshBots() {
        loading = true
        coroutineScope.launch {
            bots = ApiClient.fetchBots()
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshBots()
    }

    fun loadWebhookForSelected() {
        val bot = selectedBot ?: return
        loadingWebhook = true
        coroutineScope.launch {
            webhookInfo = ApiClient.fetchWebhookInfo(bot.token)
            loadingWebhook = false
        }
    }

    LaunchedEffect(selectedBot, viewMode) {
        if (viewMode == BotFatherMode.DETAILS && selectedBot != null) {
            loadWebhookForSelected()
        }
    }

    // Gestion de la touche retour physique
    BackHandler(enabled = true) {
        when (viewMode) {
            BotFatherMode.CREATE -> viewMode = BotFatherMode.LIST
            BotFatherMode.DETAILS -> viewMode = BotFatherMode.LIST
            BotFatherMode.LIST -> onBack()
        }
    }

    // Validation username bot
    val trimmedUsername = newBotUsername.trim()
    val isUsernameValid = remember(trimmedUsername) {
        if (trimmedUsername.length < 4) return@remember false
        val lower = trimmedUsername.lowercase()
        if (!lower.endsWith("bot")) return@remember false
        Regex("^[a-zA-Z0-9_]+$").matches(trimmedUsername)
    }

    val canCreate = newBotName.trim().isNotBlank() && isUsernameValid && !creating

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF242F3D),
        unfocusedContainerColor = Color(0xFF242F3D),
        focusedBorderColor = TelegramColors.Accent,
        unfocusedBorderColor = Color(0xFF334455),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedPlaceholderColor = TelegramColors.TextMuted,
        unfocusedPlaceholderColor = TelegramColors.TextMuted,
        cursorColor = TelegramColors.Accent
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TelegramColors.Base)
            .statusBarsPadding()
            .imePadding()
    ) {
        // En-tête / TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TelegramColors.Header)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    when (viewMode) {
                        BotFatherMode.CREATE -> viewMode = BotFatherMode.LIST
                        BotFatherMode.DETAILS -> viewMode = BotFatherMode.LIST
                        BotFatherMode.LIST -> onBack()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = when (viewMode) {
                        BotFatherMode.LIST -> "BotFather"
                        BotFatherMode.CREATE -> "Nouveau Bot"
                        BotFatherMode.DETAILS -> selectedBot?.firstName ?: "Détails du Bot"
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (viewMode) {
                        BotFatherMode.LIST -> "${bots.size} bots configurés"
                        BotFatherMode.CREATE -> "Création d'un bot Telegram"
                        BotFatherMode.DETAILS -> "@${selectedBot?.username}"
                    },
                    color = TelegramColors.TextMuted,
                    fontSize = 12.sp
                )
            }

            if (viewMode == BotFatherMode.LIST) {
                IconButton(onClick = { refreshBots() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualiser",
                        tint = Color.White
                    )
                }
            } else if (viewMode == BotFatherMode.DETAILS) {
                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer le Bot",
                        tint = Color(0xFFE17076)
                    )
                }
            }
        }

        // ================= VUE 1 : LISTE DES BOTS =================
        if (viewMode == BotFatherMode.LIST) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Barre de recherche
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        placeholder = { Text("Rechercher un bot...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Recherche",
                                tint = TelegramColors.TextMuted
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Effacer",
                                        tint = TelegramColors.TextMuted
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )

                    val filtered = remember(bots, searchQuery) {
                        if (searchQuery.isBlank()) bots
                        else {
                            val q = searchQuery.lowercase()
                            bots.filter {
                                it.firstName.lowercase().contains(q) ||
                                    it.username.lowercase().contains(q)
                            }
                        }
                    }

                    if (loading && bots.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TelegramColors.Accent)
                        }
                    } else if (filtered.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = TelegramColors.TextMuted,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Aucun bot ne correspond à votre recherche" else "Aucun bot créé pour le moment",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Créez votre premier bot Telegram pour commencer l'automatisation.",
                                color = TelegramColors.TextMuted,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewMode = BotFatherMode.CREATE },
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramColors.Accent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Créer un Bot")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(filtered, key = { it.id }) { bot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedBot = bot
                                            showToken = false
                                            viewMode = BotFatherMode.DETAILS
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val initials = bot.firstName.take(2).uppercase().ifEmpty { "BT" }
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(getAvatarColor(bot.firstName)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bot.firstName,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "@${bot.username}",
                                            color = TelegramColors.Accent,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Voir",
                                        tint = TelegramColors.TextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                HorizontalDivider(
                                    color = Color(0x22FFFFFF),
                                    modifier = Modifier.padding(start = 80.dp)
                                )
                            }
                        }
                    }
                }

                // FAB Créer Nouveau Bot
                FloatingActionButton(
                    onClick = { viewMode = BotFatherMode.CREATE },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                    containerColor = TelegramColors.Accent,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Créer un bot",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // ================= VUE 2 : CRÉATION D'UN BOT =================
        else if (viewMode == BotFatherMode.CREATE) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Créer un nouveau Bot",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Configurez les informations de base de votre bot. Vous obtiendrez un jeton d'API HTTP instantanément.",
                    color = TelegramColors.TextMuted,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Champ Nom
                Text(
                    text = "Nom du Bot *",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newBotName,
                    onValueChange = { newBotName = it },
                    placeholder = { Text("ex: Mon Assistant") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Champ Description / About
                Text(
                    text = "Description / À propos (Optionnel)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newBotAbout,
                    onValueChange = { newBotAbout = it },
                    placeholder = { Text("ex: Bot d'actualités et d'alertes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Champ Username avec validation Telegram
                Text(
                    text = "Nom d'utilisateur (Username) *",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newBotUsername,
                    onValueChange = { newBotUsername = it },
                    placeholder = { Text("ex: mon_assistant_bot") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    singleLine = true,
                    trailingIcon = {
                        if (trimmedUsername.isNotEmpty()) {
                            Icon(
                                imageVector = if (isUsernameValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isUsernameValid) Color(0xFF6ECC51) else Color(0xFFE17076)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Aide contextuelle Telegram Username
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            trimmedUsername.isEmpty() -> "Doit se terminer par 'bot' (ex: tetris_bot ou TetrisBot)"
                            trimmedUsername.length < 4 -> "Au moins 4 caractères requis"
                            !trimmedUsername.lowercase().endsWith("bot") -> "Le nom d'utilisateur doit obligatoirement se terminer par 'bot'"
                            !Regex("^[a-zA-Z0-9_]+$").matches(trimmedUsername) -> "Seules les lettres, chiffres et tirets bas (_) sont autorisés"
                            else -> "✓ Nom d'utilisateur valide !"
                        },
                        color = if (isUsernameValid) Color(0xFF6ECC51) else TelegramColors.TextMuted,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bouton Créer
                Button(
                    onClick = {
                        creating = true
                        coroutineScope.launch {
                            val created = ApiClient.createBot(
                                username = trimmedUsername,
                                firstName = newBotName.trim(),
                                about = newBotAbout.trim().ifEmpty { null }
                            )
                            creating = false
                            if (created != null) {
                                bots = listOf(created) + bots.filter { it.id != created.id }
                                selectedBot = created
                                newBotName = ""
                                newBotAbout = ""
                                newBotUsername = ""
                                viewMode = BotFatherMode.DETAILS
                                Toast.makeText(context, "✓ Bot créé avec succès !", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Échec de création du bot", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = canCreate,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramColors.Accent)
                ) {
                    if (creating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Créer le Bot",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ================= VUE 3 : DÉTAILS & GESTION DU BOT =================
        else if (viewMode == BotFatherMode.DETAILS && selectedBot != null) {
            val bot = selectedBot!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Carte En-tête Profil
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A38))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val initials = bot.firstName.take(2).uppercase().ifEmpty { "BT" }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(getAvatarColor(bot.firstName)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = bot.firstName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "@${bot.username}",
                            color = TelegramColors.Accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (!bot.about.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = bot.about,
                                color = TelegramColors.TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton Tester le Bot / Ouvrir la discussion
                Button(
                    onClick = {
                        val initials = bot.firstName.take(2).uppercase().ifEmpty { "BT" }
                        onOpenChatWithBot(bot.firstName, initials)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramColors.Accent)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Chat",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tester le Bot / Ouvrir le Chat",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Carte Jeton API HTTP
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A38))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Jeton d'accès HTTP API",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Afficher token",
                                    tint = TelegramColors.Accent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val displayToken = if (showToken) {
                            bot.token
                        } else {
                            val parts = bot.token.split(":")
                            if (parts.size == 2) "${parts[0]}:AAH••••••••••••••••••••••••••••"
                            else bot.token.take(8) + "••••••••••••••••••••••••••••"
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141E28))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = displayToken,
                                color = Color(0xFF90CAF9),
                                fontSize = 13.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(bot.token))
                                    Toast.makeText(context, "✓ Jeton copié dans le presse-papier", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copier",
                                    tint = TelegramColors.Accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copier", color = TelegramColors.Accent, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            OutlinedButton(
                                onClick = { showRevokeConfirmDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Révoquer",
                                    tint = Color(0xFFE17076),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Révoquer", color = Color(0xFFE17076), fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Carte Webhook
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A38))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Configuration Webhook",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (loadingWebhook) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = TelegramColors.Accent,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val hasWebhook = !webhookInfo?.url.isNullOrBlank()

                        Text(
                            text = if (hasWebhook) "URL Active : ${webhookInfo?.url}"
                            else "Aucun webhook actif (Long-polling / WebSockets activé)",
                            color = if (hasWebhook) Color(0xFF6ECC51) else TelegramColors.TextMuted,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    webhookUrlInput = webhookInfo?.url ?: ""
                                    webhookSecretInput = ""
                                    dropPendingUpdates = false
                                    showWebhookDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3A4C))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Configurer",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Configurer", color = Color.White, fontSize = 13.sp)
                            }

                            if (hasWebhook) {
                                Spacer(modifier = Modifier.width(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            ApiClient.deleteBotWebhook(bot.token)
                                            loadWebhookForSelected()
                                            Toast.makeText(context, "✓ Webhook supprimé", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Supprimer", color = Color(0xFFE17076), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ================= DIALOGUES =================

    // Dialogue Confirmation Révocation Token
    if (showRevokeConfirmDialog && selectedBot != null) {
        val bot = selectedBot!!
        AlertDialog(
            onDismissRequest = { showRevokeConfirmDialog = false },
            title = { Text("Révoquer le jeton ?") },
            text = {
                Text("Voulez-vous vraiment régénérer le jeton de @${bot.username} ? L'ancien jeton sera invalidé immédiatement.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRevokeConfirmDialog = false
                        coroutineScope.launch {
                            val newToken = ApiClient.revokeBotToken(bot.id)
                            if (newToken != null) {
                                selectedBot = bot.copy(token = newToken)
                                bots = bots.map { if (it.id == bot.id) it.copy(token = newToken) else it }
                                Toast.makeText(context, "✓ Nouveau jeton généré !", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE17076))
                ) {
                    Text("Révoquer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue Confirmation Suppression Bot
    if (showDeleteConfirmDialog && selectedBot != null) {
        val bot = selectedBot!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Supprimer @${bot.username} ?") },
            text = {
                Text("Cette action supprimera définitivement le bot et ses discussions associées. Cette opération est irréversible.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        coroutineScope.launch {
                            val ok = ApiClient.deleteBot(bot.id)
                            if (ok) {
                                bots = bots.filter { it.id != bot.id }
                                selectedBot = null
                                viewMode = BotFatherMode.LIST
                                Toast.makeText(context, "Bot supprimé.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE17076))
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue Configuration Webhook
    if (showWebhookDialog && selectedBot != null) {
        val bot = selectedBot!!
        AlertDialog(
            onDismissRequest = { showWebhookDialog = false },
            title = { Text("Configurer Webhook") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Entrez l'URL HTTPS vers laquelle Telegram transmettra les mises à jour :",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = webhookUrlInput,
                        onValueChange = { webhookUrlInput = it },
                        placeholder = { Text("https://mon-serveur.com/webhook") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = webhookSecretInput,
                        onValueChange = { webhookSecretInput = it },
                        placeholder = { Text("Secret Token (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Supprimer requêtes en attente", fontSize = 12.sp)
                        Switch(
                            checked = dropPendingUpdates,
                            onCheckedChange = { dropPendingUpdates = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUrl = webhookUrlInput.trim()
                        if (cleanUrl.isBlank()) {
                            Toast.makeText(context, "Veuillez saisir une URL valide", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        savingWebhook = true
                        coroutineScope.launch {
                            val (ok, err) = ApiClient.setBotWebhook(
                                botToken = bot.token,
                                url = cleanUrl,
                                secretToken = webhookSecretInput.trim().ifEmpty { null },
                                dropPendingUpdates = dropPendingUpdates
                            )
                            savingWebhook = false
                            if (ok) {
                                showWebhookDialog = false
                                loadWebhookForSelected()
                                Toast.makeText(context, "✓ Webhook configuré !", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, err ?: "Erreur webhook", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramColors.Accent)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebhookDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
