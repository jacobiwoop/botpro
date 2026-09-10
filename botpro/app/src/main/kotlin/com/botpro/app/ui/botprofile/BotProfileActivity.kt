package com.botpro.app.ui.botprofile

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.botpro.app.R

/**
 * Profil d'un bot.
 * Adapté de ProfileActivity de Telegram (16 987 lignes Java).
 * Affiche la description du bot, son nom d'utilisateur (@username),
 * les commandes disponibles et les actions directes.
 */
class BotProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bot_profile)

        val botName = intent.getStringExtra("bot_name") ?: "Bot"
        val botUsername = intent.getStringExtra("bot_username") ?: ""
        val botDescription = intent.getStringExtra("bot_description") ?: "Aucune description fournie."

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.profile_name).text = botName
        findViewById<TextView>(R.id.profile_username).text = if (botUsername.isNotEmpty()) "@$botUsername" else ""
        findViewById<TextView>(R.id.profile_description_text).text = botDescription
        findViewById<TextView>(R.id.avatar_text).text = botName.take(1).uppercase()
    }
}
