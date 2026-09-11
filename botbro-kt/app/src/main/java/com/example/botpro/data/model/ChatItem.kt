package com.example.botpro.data.model

enum class AvatarType {
    TELEGRAM,
    IMAGE,
    INITIALS,
    GETPAY,
    DOT,
    SAVED
}

data class ChatItem(
    val id: String,
    val name: String,
    val avatarType: AvatarType,
    val avatarUri: String? = null,
    val avatarBg: String? = null,
    val initials: String? = null,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isVerified: Boolean = false,
    val isRead: Boolean = false
)
