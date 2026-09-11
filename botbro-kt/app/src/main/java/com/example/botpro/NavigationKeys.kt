package com.example.botpro

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object ChatListNavKey : NavKey

@Serializable
data class ConversationNavKey(
    val name: String,
    val initials: String
) : NavKey
