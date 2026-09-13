package com.example.botpro.data.cache

import android.content.Context
import android.util.Log
import com.example.botpro.data.model.AvatarType
import com.example.botpro.data.model.ChatItem
import com.example.botpro.data.model.FileAttachment
import com.example.botpro.data.model.Message
import com.example.botpro.data.model.MessageType
import com.example.botpro.data.model.ReplyQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object ChatCache {
    private const val TAG = "ChatCache"

    // Cache L1 (RAM) pour accès synchrone instantané en 0 ms
    private val memoryMessagesCache = ConcurrentHashMap<String, List<Message>>()
    private var memoryChatsCache: List<ChatItem>? = null

    // ==========================================
    // 1. Gestion des Chats (Liste de discussions)
    // ==========================================

    fun getMemoryCachedChats(): List<ChatItem>? {
        return memoryChatsCache
    }

    suspend fun getDiskCachedChats(context: Context): List<ChatItem> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "botpro_cache_chats.json")
            if (file.exists()) {
                val jsonString = file.readText()
                val array = JSONArray(jsonString)
                val list = mutableListOf<ChatItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ChatItem(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            avatarType = try {
                                AvatarType.valueOf(obj.optString("avatarType", AvatarType.INITIALS.name))
                            } catch (e: Exception) {
                                AvatarType.INITIALS
                            },
                            avatarUri = obj.optString("avatarUri", null),
                            avatarBg = obj.optString("avatarBg", null),
                            initials = obj.optString("initials", null),
                            lastMessage = obj.optString("lastMessage", ""),
                            time = obj.optString("time", ""),
                            unreadCount = obj.optInt("unreadCount", 0),
                            isMuted = obj.optBoolean("isMuted", false),
                            isVerified = obj.optBoolean("isVerified", false),
                            isRead = obj.optBoolean("isRead", true)
                        )
                    )
                }
                memoryChatsCache = list
                return@withContext list
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur lecture disque chats: ${e.message}")
        }
        emptyList()
    }

    suspend fun saveCachedChats(context: Context, chats: List<ChatItem>) = withContext(Dispatchers.IO) {
        memoryChatsCache = chats
        try {
            val file = File(context.filesDir, "botpro_cache_chats.json")
            val array = JSONArray()
            for (chat in chats) {
                val obj = JSONObject().apply {
                    put("id", chat.id)
                    put("name", chat.name)
                    put("avatarType", chat.avatarType.name)
                    chat.avatarUri?.let { put("avatarUri", it) }
                    chat.avatarBg?.let { put("avatarBg", it) }
                    chat.initials?.let { put("initials", it) }
                    put("lastMessage", chat.lastMessage)
                    put("time", chat.time)
                    put("unreadCount", chat.unreadCount)
                    put("isMuted", chat.isMuted)
                    put("isVerified", chat.isVerified)
                    put("isRead", chat.isRead)
                }
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Erreur écriture disque chats: ${e.message}")
        }
    }

    // ==========================================
    // 2. Gestion des Messages (Fil de discussion)
    // ==========================================

    fun getMemoryCachedMessages(chatId: String): List<Message>? {
        return memoryMessagesCache[chatId]
    }

    suspend fun getDiskCachedMessages(context: Context, chatId: String): List<Message> = withContext(Dispatchers.IO) {
        try {
            val safeId = chatId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(context.filesDir, "botpro_cache_messages_$safeId.json")
            if (file.exists()) {
                val jsonString = file.readText()
                val array = JSONArray(jsonString)
                val list = mutableListOf<Message>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val typeStr = obj.optString("type", MessageType.TEXT.name)
                    val type = try {
                        MessageType.valueOf(typeStr)
                    } catch (e: Exception) {
                        MessageType.TEXT
                    }

                    var quote: ReplyQuote? = null
                    val quoteObj = obj.optJSONObject("replyQuote")
                    if (quoteObj != null) {
                        quote = ReplyQuote(
                            senderName = quoteObj.optString("senderName", ""),
                            text = quoteObj.optString("text", "")
                        )
                    }

                    var fileAttach: FileAttachment? = null
                    val fileObj = obj.optJSONObject("file")
                    if (fileObj != null) {
                        fileAttach = FileAttachment(
                            name = fileObj.optString("name", "Document"),
                            size = fileObj.optString("size", ""),
                            thumbnailUri = fileObj.optString("thumbnailUri", "")
                        )
                    }

                    list.add(
                        Message(
                            id = obj.getString("id"),
                            type = type,
                            isOutgoing = obj.optBoolean("isOutgoing", false),
                            text = obj.optString("text", null),
                            time = obj.optString("time", ""),
                            isRead = obj.optBoolean("isRead", true),
                            isDoubleCheck = obj.optBoolean("isDoubleCheck", false),
                            replyQuote = quote,
                            file = fileAttach,
                            imageUrl = obj.optString("imageUrl", null),
                            dateText = obj.optString("dateText", null)
                        )
                    )
                }
                memoryMessagesCache[chatId] = list
                return@withContext list
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur lecture disque messages pour $chatId: ${e.message}")
        }
        emptyList()
    }

    suspend fun saveCachedMessages(context: Context, chatId: String, messages: List<Message>) = withContext(Dispatchers.IO) {
        memoryMessagesCache[chatId] = messages
        try {
            val safeId = chatId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(context.filesDir, "botpro_cache_messages_$safeId.json")
            val array = JSONArray()
            for (msg in messages) {
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("type", msg.type.name)
                    put("isOutgoing", msg.isOutgoing)
                    msg.text?.let { put("text", it) }
                    put("time", msg.time)
                    put("isRead", msg.isRead)
                    put("isDoubleCheck", msg.isDoubleCheck)
                    msg.imageUrl?.let { put("imageUrl", it) }
                    msg.dateText?.let { put("dateText", it) }

                    msg.replyQuote?.let { q ->
                        put("replyQuote", JSONObject().apply {
                            put("senderName", q.senderName)
                            put("text", q.text)
                        })
                    }

                    msg.file?.let { f ->
                        put("file", JSONObject().apply {
                            put("name", f.name)
                            put("size", f.size)
                            put("thumbnailUri", f.thumbnailUri)
                        })
                    }
                }
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Erreur écriture disque messages pour $chatId: ${e.message}")
        }
    }

    suspend fun appendCachedMessage(context: Context, chatId: String, message: Message) {
        val current = memoryMessagesCache[chatId] ?: emptyList()
        if (current.none { it.id == message.id }) {
            val updated = current + message
            saveCachedMessages(context, chatId, updated)
        }
    }
}
