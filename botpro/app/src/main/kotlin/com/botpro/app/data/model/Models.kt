package com.botpro.app.data.model

import java.util.UUID

/**
 * Représente un bot dans l'application BotPro.
 * Équivalent du `TLRPC.User` de Telegram pour les bots,
 * mais complètement indépendant du protocole MTProto.
 */
data class Bot(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val username: String,
    val description: String = "",
    val avatarUrl: String? = null,
    val commands: List<BotCommand> = emptyList(),
    val isOnline: Boolean = false,
    val hasWebApp: Boolean = false,
    val webAppUrl: String? = null,
    val apiEndpoint: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Commande disponible pour un bot (ex: /start, /help).
 * Correspond au `TLRPC.TL_botCommand` de Telegram.
 */
data class BotCommand(
    val command: String,
    val description: String
)

/**
 * Message dans une conversation avec un bot.
 * Correspond au `MessageObject` de Telegram,
 * simplifié pour l'usage bot uniquement.
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val botId: String,
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val replyMarkup: ReplyMarkup? = null,
    val mediaUrl: String? = null,
    val isRead: Boolean = false
)

/**
 * Types de messages supportés.
 */
enum class MessageType {
    TEXT,
    PHOTO,
    VIDEO,
    DOCUMENT,
    VOICE,
    STICKER,
    BOT_COMMAND,
    SYSTEM
}

/**
 * Clavier inline de bot (boutons sous un message).
 * Correspond au `TLRPC.TL_replyInlineMarkup` de Telegram.
 */
data class ReplyMarkup(
    val rows: List<List<InlineButton>>
)

/**
 * Bouton inline dans un clavier de bot.
 * Correspond au `TLRPC.TL_keyboardButtonCallback` etc.
 */
data class InlineButton(
    val text: String,
    val type: InlineButtonType = InlineButtonType.CALLBACK,
    val data: String? = null,
    val url: String? = null
)

enum class InlineButtonType {
    CALLBACK,
    URL,
    WEB_APP,
    SWITCH_INLINE
}

/**
 * Conversation avec un bot (élément de la liste).
 * Correspond au `TLRPC.Dialog` de Telegram.
 */
data class Conversation(
    val bot: Bot,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
