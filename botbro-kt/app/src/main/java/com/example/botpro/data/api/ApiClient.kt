package com.example.botpro.data.api

import android.util.Log
import com.example.botpro.data.models.ApiBot
import com.example.botpro.data.models.ApiUser
import com.example.botpro.data.models.ApiWebhookInfo
import com.example.botpro.data.supabase.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApiClient {
    private const val TAG = "ApiClient"

    var currentUser: ApiUser?
        get() = SupabaseService.currentUser
        set(value) {
            if (value != null) {
                SupabaseService.currentUser = value
            }
        }

    suspend fun fetchBots(): List<ApiBot> = SupabaseService.fetchBots()

    suspend fun createBot(username: String, firstName: String, about: String?): ApiBot? =
        SupabaseService.createBot(username, firstName, about)

    suspend fun deleteBot(id: Long): Boolean =
        SupabaseService.deleteBot(id)

    suspend fun revokeBotToken(id: Long): String? =
        SupabaseService.revokeBotToken(id)

    suspend fun fetchWebhookInfo(botToken: String): ApiWebhookInfo? =
        SupabaseService.fetchWebhookInfo(botToken)

    suspend fun setBotWebhook(
        botToken: String,
        url: String,
        secretToken: String?,
        dropPendingUpdates: Boolean
    ): Pair<Boolean, String?> =
        SupabaseService.setBotWebhook(botToken, url, secretToken, dropPendingUpdates)

    suspend fun deleteBotWebhook(botToken: String): Boolean = withContext(Dispatchers.IO) {
        val res = SupabaseService.deleteBotWebhook(botToken, true)
        res.first
    }

    suspend fun login(email: String, pass: String): Pair<Boolean, String?> =
        SupabaseService.login(email, pass)

    suspend fun register(
        email: String,
        pass: String,
        firstName: String,
        lastName: String?,
        username: String?
    ): Pair<Boolean, String?> =
        SupabaseService.register(email, pass, firstName, lastName, username)
}

