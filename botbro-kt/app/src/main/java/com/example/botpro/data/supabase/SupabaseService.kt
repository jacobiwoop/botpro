package com.example.botpro.data.supabase

import android.util.Log
import com.example.botpro.data.model.AvatarType
import com.example.botpro.data.model.ChatItem
import com.example.botpro.data.model.Message
import com.example.botpro.data.model.MessageType
import com.example.botpro.data.models.ApiBot
import com.example.botpro.data.models.ApiUser
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
                            val msg = Message(
                                id = (record.id ?: System.currentTimeMillis()).toString(),
                                type = MessageType.TEXT,
                                isOutgoing = !record.isBot && (record.fromUserId == currentUser.id || (record.fromUserId != null && record.fromUserId != 2L)),
                                text = record.text,
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
                        lastMsgText = msgs[0].text
                        lastMsgTime = formatIsoTime(msgs[0].createdAt)
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
                Message(
                    id = (r.id ?: System.currentTimeMillis()).toString(),
                    type = MessageType.TEXT,
                    isOutgoing = !r.isBot && (r.fromUserId == currentUser.id || (r.fromUserId != null && r.fromUserId != 2L)),
                    text = r.text,
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

    suspend fun sendUserMessage(chatId: Long, text: String): Message? = withContext(Dispatchers.IO) {
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
            }.toString()

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload) }

            val code = conn.responseCode
            if (code in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = json.decodeFromString(DispatchMessageResponse.serializer(), resp)
                val m = parsed.message
                if (m != null) {
                    return@withContext Message(
                        id = (m.id ?: System.currentTimeMillis()).toString(),
                        type = MessageType.TEXT,
                        isOutgoing = true,
                        text = m.text,
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
            bots.map { b ->
                ApiBot(
                    id = b.id,
                    token = b.token,
                    username = if (b.id == 1L) "BotFather" else "aikobot",
                    firstName = if (b.id == 1L) "BotFather" else "Aiko AI",
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
            val cleanUser = username.lowercase().replace("@", "").trim()
            val finalUser = if (cleanUser.endsWith("bot")) cleanUser else "${cleanUser}_bot"
            val token = "bot_${System.currentTimeMillis()}_${(1000..9999).random()}"

            val created = client.from("bots").insert(
                InsertBotRow(
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
                username = finalUser,
                firstName = firstName,
                about = about
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
}
