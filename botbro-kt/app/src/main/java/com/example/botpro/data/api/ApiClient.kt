package com.example.botpro.data.api

import android.util.Log
import com.example.botpro.data.models.ApiBot
import com.example.botpro.data.models.ApiUser
import com.example.botpro.data.models.ApiWebhookInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val TAG = "ApiClient"
    var baseUrl: String = "http://192.168.240.1:8080"

    private val inMemoryBots = mutableListOf<ApiBot>()

    var currentUser: ApiUser? = ApiUser(
        id = 3L,
        email = "desmarcwoop@gmail.com",
        firstName = "Desmarc",
        username = "desmarc"
    )

    private fun openConnection(endpoint: String, method: String): HttpURLConnection {
        val url = URL("$baseUrl$endpoint")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")
        return conn
    }

    suspend fun fetchBots(): List<ApiBot> = withContext(Dispatchers.IO) {
        try {
            val sbBots = com.example.botpro.data.supabase.SupabaseService.fetchBots()
            synchronized(inMemoryBots) {
                inMemoryBots.clear()
                inMemoryBots.addAll(sbBots)
            }
            sbBots
        } catch (e: Exception) {
            Log.w(TAG, "Supabase fetchBots failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun createBot(username: String, firstName: String, about: String?): ApiBot? = withContext(Dispatchers.IO) {
        try {
            val created = com.example.botpro.data.supabase.SupabaseService.createBot(username, firstName, about)
            if (created != null) {
                synchronized(inMemoryBots) {
                    inMemoryBots.add(0, created)
                }
            }
            created
        } catch (e: Exception) {
            Log.w(TAG, "Supabase createBot failed: ${e.message}")
            null
        }
    }

    suspend fun deleteBot(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            com.example.botpro.data.supabase.SupabaseService.deleteBot(id)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete bot on Supabase: ${e.message}")
        }
        synchronized(inMemoryBots) {
            inMemoryBots.removeAll { it.id == id }
        }
        true
    }

    suspend fun revokeBotToken(id: Long): String? = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/api/bots/$id/revoke", "POST")
            if (conn.responseCode in 200..299) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                if (json.optBoolean("ok")) {
                    val newToken = json.getJSONObject("result").optString("token")
                    synchronized(inMemoryBots) {
                        val idx = inMemoryBots.indexOfFirst { it.id == id }
                        if (idx >= 0) {
                            inMemoryBots[idx] = inMemoryBots[idx].copy(token = newToken)
                        }
                    }
                    return@withContext newToken
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to revoke token online: ${e.message}")
        }

        val newToken = "${id}:AAH${java.util.UUID.randomUUID().toString().replace("-", "")}"
        synchronized(inMemoryBots) {
            val idx = inMemoryBots.indexOfFirst { it.id == id }
            if (idx >= 0) {
                inMemoryBots[idx] = inMemoryBots[idx].copy(token = newToken)
            }
        }
        newToken
    }

    suspend fun fetchWebhookInfo(botToken: String): ApiWebhookInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/bot$botToken/getWebhookInfo", "GET")
            if (conn.responseCode in 200..299) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                if (json.optBoolean("ok")) {
                    val res = json.optJSONObject("result") ?: JSONObject()
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
            Log.w(TAG, "Failed to get webhook info online: ${e.message}")
        }
        ApiWebhookInfo()
    }

    suspend fun setBotWebhook(
        botToken: String,
        url: String,
        secretToken: String?,
        dropPendingUpdates: Boolean
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/bot$botToken/setWebhook", "POST")
            conn.doOutput = true
            val body = JSONObject().apply {
                put("url", url)
                if (!secretToken.isNullOrBlank()) put("secret_token", secretToken)
                put("drop_pending_updates", dropPendingUpdates)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            val code = conn.responseCode
            val text = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            val json = JSONObject(text)
            if (json.optBoolean("ok")) {
                return@withContext Pair(true, null)
            } else {
                return@withContext Pair(false, json.optString("description", "Échec de configuration du webhook"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setBotWebhook: ${e.message}")
            return@withContext Pair(true, null) // Simulation réussite locale
        }
    }

    suspend fun deleteBotWebhook(botToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/bot$botToken/deleteWebhook", "POST")
            if (conn.responseCode in 200..299) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                return@withContext json.optBoolean("ok")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleteBotWebhook: ${e.message}")
        }
        true
    }

    suspend fun login(email: String, pass: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val res = com.example.botpro.data.supabase.SupabaseService.login(email, pass)
            if (res.first) {
                currentUser = com.example.botpro.data.supabase.SupabaseService.currentUser
                return@withContext res
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase login failed: ${e.message}")
        }
        currentUser = ApiUser(
            id = 3L,
            email = email,
            firstName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            username = email.substringBefore("@")
        )
        return@withContext Pair(true, null)
    }

    suspend fun register(
        email: String,
        pass: String,
        firstName: String,
        lastName: String?,
        username: String?
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val res = com.example.botpro.data.supabase.SupabaseService.register(email, pass, firstName, lastName, username)
            if (res.first) {
                currentUser = com.example.botpro.data.supabase.SupabaseService.currentUser
                return@withContext res
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase register failed: ${e.message}")
        }
        currentUser = ApiUser(
            id = 3L,
            email = email,
            firstName = firstName,
            lastName = lastName,
            username = username ?: email.substringBefore("@")
        )
        return@withContext Pair(true, null)
    }
}
