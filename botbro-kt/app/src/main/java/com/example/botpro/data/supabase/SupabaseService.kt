package com.example.botpro.data.supabase

import android.util.Log
import com.example.botpro.data.model.AvatarType
import com.example.botpro.data.model.ChatItem
import com.example.botpro.data.model.FileAttachment
import com.example.botpro.data.model.Message
import com.example.botpro.data.model.MessageType
import com.example.botpro.data.models.ApiBot
import com.example.botpro.data.models.ApiUser
import com.example.botpro.data.models.ApiWebhookInfo
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SupabaseService {
    private const val TAG = "SupabaseService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    var currentUser: ApiUser = ApiUser(
        id = 3L,
        email = "desmarcwoop@gmail.com",
        firstName = "Desmarc",
        username = "desmarc"
    )

    private val _realtimeMessages = MutableSharedFlow<Pair<Long, Message>>(extraBufferCapacity = 64)
    val realtimeMessages: SharedFlow<Pair<Long, Message>> = _realtimeMessages.asSharedFlow()

    private var isRealtimeSubscribed = false

    init {
        initRealtimeSubscription()
    }

    private fun initRealtimeSubscription() {
        if (isRealtimeSubscribed) return
        isRealtimeSubscribed = true
        scope.launch {
            try {
                val client = SupabaseClientProvider.client
                val channel = client.channel("public:messages")
                val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                }

                launch {
                    changeFlow.collect { action ->
                        try {
                            val record = json.decodeFromJsonElement(SupabaseMessageRow.serializer(), action.record)
                            val chatId = record.chatId
                            val msgType = when (record.mediaType?.lowercase()) {
                                "photo", "image" -> MessageType.IMAGE
                                "document", "file" -> MessageType.FILE
                                "audio", "voice" -> MessageType.VOICE
                                else -> MessageType.TEXT
                            }
                            val fileAtt = if (msgType == MessageType.FILE) {
                                FileAttachment(
                                    name = record.fileName ?: "Document",
                                    size = record.fileSize ?: "Fichier",
                                    thumbnailUri = record.mediaUrl ?: ""
                                )
                            } else null

                            val msg = Message(
                                id = (record.id ?: System.currentTimeMillis()).toString(),
                                type = msgType,
                                isOutgoing = !record.isBot && (record.fromUserId == currentUser.id || (record.fromUserId != null && record.fromUserId != 2L)),
                                text = record.text ?: (if (msgType == MessageType.IMAGE) "Photo 📸" else ""),
                                imageUrl = if (msgType == MessageType.IMAGE) record.mediaUrl else null,
                                file = fileAtt,
                                time = formatIsoTime(record.createdAt),
                                isRead = true,
                                isDoubleCheck = true
                            )
                            Log.d(TAG, "Realtime message received for chat $chatId: ${msg.text}")
                            _realtimeMessages.emit(Pair(chatId, msg))
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse realtime insert: ${e.message}", e)
                        }
                    }
                }

                channel.subscribe()
                Log.i(TAG, "Subscribed to Supabase Realtime 'public:messages'")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize Supabase Realtime: ${e.message}")
            }
        }
    }

    fun formatIsoTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "12:00"
        return try {
            val odt = OffsetDateTime.parse(isoString)
            val local = odt.atZoneSameInstant(ZoneId.systemDefault())
            local.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            try {
                isoString.substringAfter("T").take(5)
            } catch (e2: Exception) {
                "12:00"
            }
        }
    }

    suspend fun login(email: String, pass: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            client.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            currentUser = ApiUser(
                id = 3L,
                email = email,
                firstName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                username = email.substringBefore("@")
            )
            Pair(true, null)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase login error: ${e.message}")
            Pair(false, e.message ?: "Échec de connexion Supabase")
        }
    }

    suspend fun register(
        email: String,
        pass: String,
        firstName: String,
        lastName: String?,
        username: String?
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
                data = buildJsonObject {
                    put("first_name", firstName)
                    if (!lastName.isNullOrBlank()) put("last_name", lastName)
                    put("username", username ?: email.substringBefore("@"))
                }
            }
            currentUser = ApiUser(
                id = 3L,
                email = email,
                firstName = firstName,
                lastName = lastName,
                username = username ?: email.substringBefore("@")
            )
            Pair(true, null)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase register error: ${e.message}")
            Pair(false, e.message ?: "Échec d'inscription Supabase")
        }
    }

    suspend fun fetchChats(): List<ChatItem> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val chatRows = client.from("chats").select().decodeList<SupabaseChatRow>()
            val chatItems = mutableListOf<ChatItem>()

            for (c in chatRows) {
                // Récupérer le dernier message pour chaque chat
                var lastMsgText = "Nouvelle conversation"
                var lastMsgTime = "12:00"
                try {
                    val msgs = client.from("messages").select {
                        filter {
                            eq("chat_id", c.id)
                        }
                        order("id", Order.DESCENDING)
                        limit(1L)
                    }.decodeList<SupabaseMessageRow>()

                    if (msgs.isNotEmpty()) {
                        val m = msgs[0]
                        lastMsgText = when (m.mediaType?.lowercase()) {
                            "photo", "image" -> "📷 Photo"
                            "document", "file" -> "📎 Document"
                            "audio", "voice" -> "🎤 Message vocal"
                            else -> m.text ?: "Message"
                        }
                        lastMsgTime = formatIsoTime(m.createdAt)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load latest msg for chat ${c.id}: ${e.message}")
                }

                val isBf = (c.username?.equals("BotFather", ignoreCase = true) == true) || (c.title?.contains("BotFather", ignoreCase = true) == true)
                chatItems.add(
                    ChatItem(
                        id = c.id.toString(),
                        name = c.title ?: (c.username ?: "Chat ${c.id}"),
                        avatarType = if (isBf) AvatarType.TELEGRAM else AvatarType.INITIALS,
                        avatarBg = if (isBf) "#0088CC" else "#5288C1",
                        initials = (c.title ?: c.username ?: "CH").take(2).uppercase(),
                        lastMessage = lastMsgText,
                        time = lastMsgTime,
                        unreadCount = 0,
                        isRead = true
                    )
                )
            }
            chatItems
        } catch (e: Exception) {
            Log.e(TAG, "Error fetchChats: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchMessages(chatId: Long): List<Message> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val rows = client.from("messages").select {
                filter {
                    eq("chat_id", chatId)
                }
                order("id", Order.ASCENDING)
            }.decodeList<SupabaseMessageRow>()

            rows.map { r ->
                val msgType = when (r.mediaType?.lowercase()) {
                    "photo", "image" -> MessageType.IMAGE
                    "document", "file" -> MessageType.FILE
                    "audio", "voice" -> MessageType.VOICE
                    else -> MessageType.TEXT
                }
                val fileAtt = if (msgType == MessageType.FILE) {
                    FileAttachment(
                        name = r.fileName ?: "Document",
                        size = r.fileSize ?: "Fichier",
                        thumbnailUri = r.mediaUrl ?: ""
                    )
                } else null

                Message(
                    id = (r.id ?: System.currentTimeMillis()).toString(),
                    type = msgType,
                    isOutgoing = !r.isBot && (r.fromUserId == currentUser.id || (r.fromUserId != null && r.fromUserId != 2L)),
                    text = r.text ?: (if (msgType == MessageType.IMAGE) "Photo 📸" else ""),
                    imageUrl = if (msgType == MessageType.IMAGE) r.mediaUrl else null,
                    file = fileAtt,
                    time = formatIsoTime(r.createdAt),
                    isRead = true,
                    isDoubleCheck = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetchMessages for chat $chatId: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun uploadChatMedia(
        context: android.content.Context,
        uri: android.net.Uri,
        mimeType: String = "image/jpeg",
        customFileName: String? = null
    ): Triple<String, String, String>? = withContext(Dispatchers.IO) {
        try {
            val ext = if (customFileName != null && customFileName.contains(".")) {
                customFileName.substringAfterLast(".")
            } else if (mimeType.contains("png")) {
                "png"
            } else if (mimeType.contains("pdf")) {
                "pdf"
            } else {
                "jpg"
            }
            val finalName = customFileName ?: "media_${System.currentTimeMillis()}.$ext"
            val filePath = "uploads/${System.currentTimeMillis()}_$finalName"
            val uploadUrl = "${SupabaseClientProvider.SUPABASE_URL}/storage/v1/object/chat-media/$filePath"

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Log.w(TAG, "Empty file bytes for URI: $uri")
                return@withContext null
            }

            val sizeKb = bytes.size / 1024
            val sizeStr = if (sizeKb > 1024) String.format(java.util.Locale.US, "%.1f MB", sizeKb / 1024.0) else "$sizeKb KB"

            val conn = URL(uploadUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.setRequestProperty("Content-Type", mimeType)
            conn.setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.SUPABASE_ANON_KEY}")
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(bytes)
                os.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val publicUrl = "${SupabaseClientProvider.SUPABASE_URL}/storage/v1/object/public/chat-media/$filePath"
                Log.i(TAG, "Upload success: $publicUrl")
                return@withContext Triple(publicUrl, finalName, sizeStr)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "Storage upload failed with code $code: $err")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading chat media: ${e.message}", e)
            null
        }
    }

    suspend fun sendUserMessage(
        chatId: Long,
        text: String,
        mediaType: String = "text",
        mediaUrl: String? = null,
        fileName: String? = null,
        fileSize: String? = null
    ): Message? = withContext(Dispatchers.IO) {
        try {
            val url = URL(SupabaseClientProvider.DISPATCH_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.SUPABASE_ANON_KEY}")
            conn.doOutput = true

            val payload = buildJsonObject {
                put("chat_id", chatId)
                put("user_id", currentUser.id)
                put("text", text)
                put("media_type", mediaType)
                if (mediaUrl != null) put("media_url", mediaUrl)
                if (fileName != null) put("file_name", fileName)
                if (fileSize != null) put("file_size", fileSize)
            }.toString()

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload) }

            val code = conn.responseCode
            if (code in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = json.decodeFromString(DispatchMessageResponse.serializer(), resp)
                val m = parsed.message
                if (m != null) {
                    val msgType = when (m.mediaType?.lowercase()) {
                        "photo", "image" -> MessageType.IMAGE
                        "document", "file" -> MessageType.FILE
                        "audio", "voice" -> MessageType.VOICE
                        else -> MessageType.TEXT
                    }
                    val fileAtt = if (msgType == MessageType.FILE) {
                        FileAttachment(
                            name = m.fileName ?: "Document",
                            size = m.fileSize ?: "Fichier",
                            thumbnailUri = m.mediaUrl ?: ""
                        )
                    } else null

                    return@withContext Message(
                        id = (m.id ?: System.currentTimeMillis()).toString(),
                        type = msgType,
                        isOutgoing = true,
                        text = m.text ?: (if (msgType == MessageType.IMAGE) "Photo 📸" else ""),
                        imageUrl = if (msgType == MessageType.IMAGE) m.mediaUrl else null,
                        file = fileAtt,
                        time = formatIsoTime(m.createdAt),
                        isRead = true,
                        isDoubleCheck = true
                    )
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "dispatch-user-message error $code: $err")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching user message: ${e.message}", e)
        }
        null
    }

    suspend fun fetchBots(): List<ApiBot> = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val bots = client.from("bots").select().decodeList<SupabaseBotRow>()
            val users = try {
                client.from("users").select().decodeList<SupabaseUserRow>()
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching users for bots: ${e.message}")
                emptyList()
            }
            val usersMap = users.associateBy { it.id }

            bots.map { b ->
                val u = usersMap[b.id]
                ApiBot(
                    id = b.id,
                    token = b.token,
                    username = u?.username ?: (if (b.id == 1L) "BotFather" else "bot_${b.id}"),
                    firstName = u?.firstName ?: (if (b.id == 1L) "BotFather" else "Bot"),
                    about = b.description ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetchBots from Supabase: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createBot(username: String, firstName: String, about: String?): ApiBot? = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val cleanUser = username.lowercase().replace("@", "").replace(" ", "").trim()
            val finalUser = if (cleanUser.endsWith("bot")) cleanUser else "${cleanUser}_bot"
            val token = "bot_${System.currentTimeMillis()}_${(1000..9999).random()}"

            // 1. Créer l'utilisateur bot dans la table 'users'
            val botUser = client.from("users").insert(
                InsertUserRow(
                    username = finalUser,
                    firstName = firstName,
                    isBot = true
                )
            ) {
                select()
            }.decodeSingle<SupabaseUserRow>()

            // 2. Insérer dans la table 'bots' avec id = botUser.id
            val created = client.from("bots").insert(
                InsertBotRowWithId(
                    id = botUser.id,
                    token = token,
                    description = about ?: "Bot créé via BotFather",
                    ownerId = currentUser.id
                )
            ) {
                select()
            }.decodeSingle<SupabaseBotRow>()

            ApiBot(
                id = created.id,
                token = created.token,
                username = botUser.username ?: finalUser,
                firstName = botUser.firstName ?: firstName,
                about = about ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error createBot on Supabase: ${e.message}", e)
            null
        }
    }

    suspend fun deleteBot(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            client.from("bots").delete {
                filter {
                    eq("id", id)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleteBot $id: ${e.message}", e)
            false
        }
    }

    suspend fun revokeBotToken(id: Long): String? = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val newToken = "bot_${System.currentTimeMillis()}_${(1000..9999).random()}"
            client.from("bots").update(
                UpdateBotTokenRow(token = newToken)
            ) {
                filter {
                    eq("id", id)
                }
            }
            newToken
        } catch (e: Exception) {
            Log.e(TAG, "Error revoking token for bot $id: ${e.message}", e)
            null
        }
    }

    suspend fun getOrCreateBotChat(botId: Long, botName: String, botUsername: String): Long = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClientProvider.client
            val currentUserId = currentUser.id

            // 1. Chercher les chats de l'utilisateur actuel
            val myMemberships = client.from("chat_members").select {
                filter {
                    eq("user_id", currentUserId)
                }
            }.decodeList<SupabaseChatMemberRow>()

            if (myMemberships.isNotEmpty()) {
                val myChatIds = myMemberships.map { it.chatId }.toSet()
                // 2. Chercher les chats du bot
                val botMemberships = client.from("chat_members").select {
                    filter {
                        eq("user_id", botId)
                    }
                }.decodeList<SupabaseChatMemberRow>()

                val common = botMemberships.firstOrNull { it.chatId in myChatIds }
                if (common != null) {
                    return@withContext common.chatId
                }
            }

            // 3. Pas de chat trouvé : en créer un nouveau
            val newChat = client.from("chats").insert(
                InsertChatRow(
                    type = "private",
                    title = botName,
                    username = botUsername
                )
            ) {
                select()
            }.decodeSingle<SupabaseChatRow>()

            // 4. Insérer les deux participants dans chat_members
            client.from("chat_members").insert(
                listOf(
                    InsertChatMemberRow(chatId = newChat.id, userId = currentUserId, role = "creator"),
                    InsertChatMemberRow(chatId = newChat.id, userId = botId, role = "member")
                )
            )

            newChat.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getOrCreateBotChat for bot $botId: ${e.message}", e)
            1L
        }
    }

    suspend fun fetchWebhookInfo(botToken: String): ApiWebhookInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseClientProvider.TELEGRAM_EDGE_URL}/bot$botToken/getWebhookInfo")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.SUPABASE_ANON_KEY}")
            if (conn.responseCode in 200..299) {
                val json = org.json.JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                if (json.optBoolean("ok")) {
                    val res = json.optJSONObject("result") ?: org.json.JSONObject()
                    return@withContext ApiWebhookInfo(
                        url = res.optString("url", ""),
                        hasCustomCertificate = res.optBoolean("has_custom_certificate", false),
                        pendingUpdateCount = res.optInt("pending_update_count", 0),
                        lastErrorDate = if (res.has("last_error_date")) res.optLong("last_error_date") else null,
                        lastErrorMessage = res.optString("last_error_message", null)
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get webhook info from Edge: ${e.message}")
        }
        null
    }

    suspend fun setBotWebhook(
        botToken: String,
        url: String,
        secretToken: String?,
        dropPendingUpdates: Boolean
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("${SupabaseClientProvider.TELEGRAM_EDGE_URL}/bot$botToken/setWebhook").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.SUPABASE_ANON_KEY}")
            conn.doOutput = true

            val body = org.json.JSONObject().apply {
                put("url", url)
                if (!secretToken.isNullOrBlank()) put("secret_token", secretToken)
                put("drop_pending_updates", dropPendingUpdates)
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body.toString()) }
            val code = conn.responseCode
            val text = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            val json = org.json.JSONObject(text)
            val ok = json.optBoolean("ok", false)
            val desc = json.optString("description", if (ok) "Webhook configuré avec succès" else "Erreur")
            Pair(ok, desc)
        } catch (e: Exception) {
            Log.e(TAG, "Error setBotWebhook: ${e.message}", e)
            Pair(false, e.message ?: "Erreur réseau")
        }
    }

    suspend fun deleteBotWebhook(
        botToken: String,
        dropPendingUpdates: Boolean
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("${SupabaseClientProvider.TELEGRAM_EDGE_URL}/bot$botToken/deleteWebhook").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.SUPABASE_ANON_KEY}")
            conn.doOutput = true

            val body = org.json.JSONObject().apply {
                put("drop_pending_updates", dropPendingUpdates)
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body.toString()) }
            val code = conn.responseCode
            val text = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            val json = org.json.JSONObject(text)
            val ok = json.optBoolean("ok", false)
            val desc = json.optString("description", if (ok) "Webhook supprimé avec succès" else "Erreur")
            Pair(ok, desc)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleteBotWebhook: ${e.message}", e)
            Pair(false, e.message ?: "Erreur réseau")
        }
    }
}
