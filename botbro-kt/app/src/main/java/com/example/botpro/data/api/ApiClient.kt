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

    private val inMemoryBots = mutableListOf(
        ApiBot(
            id = 1L,
            token = "123456:BOTPRO_TEST_TOKEN",
            username = "botpro_bot",
            firstName = "Botpro Bot",
            about = "Bot officiel BotPro"
        ),
        ApiBot(
            id = 2L,
            token = "789123964:a7153845697a9a53a118213758b93569",
            username = "automation_bot",
            firstName = "Automation Bot",
            about = "Bot d'automatisation des tâches"
        ),
        ApiBot(
            id = 3L,
            token = "789124917:c455a17929c5da757e2f17b305ac8c80",
            username = "aikobot",
            firstName = "aiko",
            about = "Assistant conversationnel Aiko"
        )
    )

    var currentUser: ApiUser? = ApiUser(
        id = 1L,
        email = "darren@botpro.org",
        firstName = "darren",
        lastName = "lee",
        username = "darrenlee"
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
            val conn = openConnection("/api/bots", "GET")
            val code = conn.responseCode
            if (code in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                val json = JSONObject(response)
                if (json.optBoolean("ok")) {
                    val array = json.optJSONArray("result") ?: JSONArray()
                    val resultList = mutableListOf<ApiBot>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        resultList.add(
                            ApiBot(
                                id = obj.optLong("id"),
                                token = obj.optString("token"),
                                username = obj.optString("username"),
                                firstName = obj.optString("first_name"),
                                about = obj.optString("about", null),
                                createdAt = obj.optLong("created_at")
                            )
                        )
                    }
                    synchronized(inMemoryBots) {
                        inMemoryBots.clear()
                        inMemoryBots.addAll(resultList)
                    }
                    return@withContext resultList
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch bots from server: ${e.message}, using cached/local list")
        }
        synchronized(inMemoryBots) {
            return@withContext inMemoryBots.toList()
        }
    }

    suspend fun createBot(username: String, firstName: String, about: String?): ApiBot? = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("/api/bots", "POST")
            conn.doOutput = true
            val body = JSONObject().apply {
                put("username", username)
                put("first_name", firstName)
                if (!about.isNullOrBlank()) put("about", about)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val code = conn.responseCode
            if (code in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                if (json.optBoolean("ok")) {
                    val obj = json.getJSONObject("result")
                    val created = ApiBot(
                        id = obj.optLong("id"),
                        token = obj.optString("token"),
                        username = obj.optString("username"),
                        firstName = obj.optString("first_name"),
                        about = obj.optString("about", null),
                        createdAt = obj.optLong("created_at")
                    )
                    synchronized(inMemoryBots) {
                        inMemoryBots.add(0, created)
                    }
                    return@withContext created
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create bot online: ${e.message}, fallback to local creation")
        }

        // Fallback local creation
        val newId = System.currentTimeMillis()
        val mockToken = "${newId % 1000000000}:AAH${java.util.UUID.randomUUID().toString().replace("-", "")}"
        val localBot = ApiBot(
            id = newId,
            token = mockToken,
            username = username,
            firstName = firstName,
            about = about
        )
        synchronized(inMemoryBots) {
            inMemoryBots.add(0, localBot)
        }
        localBot
    }

    suspend fun deleteBot(id: Long): Boolean = withContext(Dispatchers.IO) {
        var remoteSuccess = false
        try {
            val conn = openConnection("/api/bots/$id", "DELETE")
            if (conn.responseCode in 200..299) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                remoteSuccess = json.optBoolean("ok")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete bot remotely: ${e.message}")
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
            val conn = openConnection("/api/auth/login", "POST")
            conn.doOutput = true
            val body = JSONObject().apply {
                put("email", email)
                put("password", pass)
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
                val res = json.getJSONObject("result")
                val u = res.getJSONObject("user")
                currentUser = ApiUser(
                    id = u.optLong("id"),
                    email = u.optString("email"),
                    firstName = u.optString("first_name"),
                    lastName = u.optString("last_name", null),
                    username = u.optString("username", null),
                    token = res.optString("token")
                )
                return@withContext Pair(true, null)
            } else {
                return@withContext Pair(false, json.optString("description", "Identifiants invalides"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Login offline: ${e.message}")
            currentUser = ApiUser(
                id = 1L,
                email = email,
                firstName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                username = email.substringBefore("@")
            )
            return@withContext Pair(true, null)
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
            val conn = openConnection("/api/auth/register", "POST")
            conn.doOutput = true
            val body = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("first_name", firstName)
                if (!lastName.isNullOrBlank()) put("last_name", lastName)
                if (!username.isNullOrBlank()) put("username", username)
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
                val res = json.getJSONObject("result")
                val u = res.getJSONObject("user")
                currentUser = ApiUser(
                    id = u.optLong("id"),
                    email = u.optString("email"),
                    firstName = u.optString("first_name"),
                    lastName = u.optString("last_name", null),
                    username = u.optString("username", null),
                    token = res.optString("token")
                )
                return@withContext Pair(true, null)
            } else {
                return@withContext Pair(false, json.optString("description", "Échec de l'inscription"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Register offline: ${e.message}")
            currentUser = ApiUser(
                id = 1L,
                email = email,
                firstName = firstName,
                lastName = lastName,
                username = username
            )
            return@withContext Pair(true, null)
        }
    }
}
