package com.example.botpro.data.model

enum class MessageType {
    TEXT,
    FILE,
    IMAGE,
    DATE_SEPARATOR
}

data class ReplyQuote(
    val senderName: String,
    val text: String
)

data class FileAttachment(
    val name: String,
    val size: String,
    val thumbnailUri: String
)

data class Message(
    val id: String,
    val type: MessageType = MessageType.TEXT,
    val isOutgoing: Boolean = false,
    val text: String? = null,
    val time: String = "",
    val isRead: Boolean = false,
    val isDoubleCheck: Boolean = false,
    val replyQuote: ReplyQuote? = null,
    val file: FileAttachment? = null,
    val imageUrl: String? = null,
    val dateText: String? = null
)
