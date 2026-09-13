package com.example.botpro.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseChatRow(
    val id: Long,
    val title: String? = null,
    val username: String? = null,
    val type: String = "private",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseMessageRow(
    val id: Long? = null,
    @SerialName("message_id") val messageId: Long? = null,
    @SerialName("chat_id") val chatId: Long,
    @SerialName("from_user_id") val fromUserId: Long? = null,
    val text: String? = null,
    @SerialName("media_type") val mediaType: String? = "text",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: String? = null,
    @SerialName("is_bot") val isBot: Boolean = false,
    @SerialName("reply_to_message_id") val replyToMessageId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseBotRow(
    val id: Long,
    val token: String,
    val description: String? = null,
    @SerialName("owner_id") val ownerId: Long? = null
)

@Serializable
data class InsertBotRow(
    val token: String,
    val description: String,
    @SerialName("owner_id") val ownerId: Long? = null
)

@Serializable
data class InsertBotRowWithId(
    val id: Long,
    val token: String,
    val description: String,
    @SerialName("owner_id") val ownerId: Long? = null
)

@Serializable
data class UpdateBotTokenRow(
    val token: String
)

@Serializable
data class InsertUserRow(
    val username: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("is_bot") val isBot: Boolean = true
)

@Serializable
data class InsertChatRow(
    val type: String = "private",
    val title: String? = null,
    val username: String? = null
)

@Serializable
data class InsertChatMemberRow(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("user_id") val userId: Long,
    val role: String = "member"
)

@Serializable
data class SupabaseChatMemberRow(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("user_id") val userId: Long,
    val role: String? = null
)

@Serializable
data class SupabaseUserRow(
    val id: Long,
    @SerialName("auth_user_id") val authUserId: String? = null,
    val username: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null,
    @SerialName("is_bot") val isBot: Boolean = false
)

@Serializable
data class DispatchMessagePayload(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("user_id") val userId: Long,
    val text: String? = "",
    @SerialName("media_type") val mediaType: String? = "text",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: String? = null
)

@Serializable
data class DispatchMessageResponse(
    val ok: Boolean = false,
    val message: SupabaseMessageRow? = null,
    @SerialName("direct_reply") val directReply: SupabaseMessageRow? = null
)
