package com.example.botpro.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiBot(
    val id: Long,
    val token: String,
    val username: String,
    val firstName: String,
    val about: String? = null,
    val createdAt: Long = System.currentTimeMillis() / 1000
)

@Serializable
data class ApiWebhookInfo(
    val url: String = "",
    val hasCustomCertificate: Boolean = false,
    val pendingUpdateCount: Int = 0,
    val lastErrorDate: Long? = null,
    val lastErrorMessage: String? = null
)

@Serializable
data class ApiUser(
    val id: Long,
    val email: String,
    val firstName: String,
    val lastName: String? = null,
    val username: String? = null,
    val token: String? = null
)
