package com.botpro.app.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpro.app.R
import com.botpro.app.data.model.*

/**
 * Écran de conversation avec un bot.
 * Basé sur `ChatActivity` de Telegram (47 254 lignes Java),
 * réécrit en Kotlin avec les patterns visuels identiques :
 * - Fond de chat sombre
 * - Bulles de messages (entrantes = sombre, sortantes = bleues)
 * - Barre de saisie en bas avec icône emoji, attach, send
 * - Clavier inline de bot (boutons sous les messages)
 * - Barre d'en-tête avec avatar, nom et statut du bot
 */
class BotChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageView
    private lateinit var toolbarTitle: TextView
    private lateinit var toolbarSubtitle: TextView
    private lateinit var backButton: ImageView
    private lateinit var botKeyboardContainer: LinearLayout
    private lateinit var adapter: ChatMessagesAdapter

    private var botId: String = ""
    private var botName: String = ""
    private var botUsername: String = ""
    private var botOnline: Boolean = false

    // Messages de démonstration
    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bot_chat)

        // Récupérer les infos du bot
        botId = intent.getStringExtra("bot_id") ?: ""
        botName = intent.getStringExtra("bot_name") ?: "Bot"
        botUsername = intent.getStringExtra("bot_username") ?: ""
        botOnline = intent.getBooleanExtra("bot_online", false)

        initViews()
        setupToolbar()
        setupChat()
        loadDemoMessages()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.chat_recycler_view)
        inputField = findViewById(R.id.chat_input_field)
        sendButton = findViewById(R.id.btn_send)
        toolbarTitle = findViewById(R.id.toolbar_title)
        toolbarSubtitle = findViewById(R.id.toolbar_subtitle)
        backButton = findViewById(R.id.btn_back)
        botKeyboardContainer = findViewById(R.id.bot_keyboard_container)
    }

    private fun setupToolbar() {
        // En-tête identique à ChatActivity de Telegram
        toolbarTitle.text = botName
        toolbarSubtitle.text = if (botOnline) getString(R.string.chat_online) else getString(R.string.chat_offline)
        toolbarSubtitle.setTextColor(
            if (botOnline) getColor(R.color.status_online)
            else getColor(R.color.text_tertiary)
        )

        backButton.setOnClickListener { finish() }

        val openProfile = View.OnClickListener {
            val intent = android.content.Intent(this, com.botpro.app.ui.botprofile.BotProfileActivity::class.java).apply {
                putExtra("bot_name", botName)
                putExtra("bot_username", botUsername)
                putExtra("bot_description", "Assistant robotisé configuré pour BotPro.")
            }
            startActivity(intent)
        }
        findViewById<View>(R.id.toolbar_avatar_container)?.setOnClickListener(openProfile)
        toolbarTitle.setOnClickListener(openProfile)
        toolbarSubtitle.setOnClickListener(openProfile)
    }

    private fun setupChat() {
        // Configuration du RecyclerView (comme ChatActivity)
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Messages les plus récents en bas
        }
        recyclerView.layoutManager = layoutManager

        adapter = ChatMessagesAdapter(messages) { button ->
            when (button.type) {
                InlineButtonType.CALLBACK -> {
                    button.data?.let { cmd -> sendMessage(cmd) }
                }
                InlineButtonType.WEB_APP -> {
                    val intent = android.content.Intent(this, com.botpro.app.ui.botwebview.BotWebViewActivity::class.java).apply {
                        putExtra("url", button.url ?: "https://telegram.org")
                        putExtra("title", button.text)
                    }
                    startActivity(intent)
                }
                InlineButtonType.URL -> {
                    button.url?.let { url ->
                        val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        startActivity(browserIntent)
                    }
                }
                InlineButtonType.SWITCH_INLINE -> {}
            }
        }
        recyclerView.adapter = adapter

        // Bouton envoyer
        sendButton.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                inputField.text.clear()
            }
        }
    }

    private fun sendMessage(text: String) {
        // Ajouter le message sortant
        val outMessage = Message(
            botId = botId,
            text = text,
            isOutgoing = true,
            type = if (text.startsWith("/")) MessageType.BOT_COMMAND else MessageType.TEXT
        )
        messages.add(outMessage)
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)

        // Simuler une réponse du bot après un court délai
        recyclerView.postDelayed({
            val botReply = Message(
                botId = botId,
                text = generateBotReply(text),
                isOutgoing = false,
                replyMarkup = generateInlineKeyboard(text)
            )
            messages.add(botReply)
            adapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }, 800)
    }

    private fun generateBotReply(userMessage: String): String {
        return when {
            userMessage.equals("/start", ignoreCase = true) ->
                "👋 Bienvenue dans $botName !\n\nJe suis prêt à vous aider. Utilisez /help pour voir les commandes disponibles."
            userMessage.equals("/help", ignoreCase = true) ->
                "📖 Commandes disponibles :\n\n/start — Démarrer\n/help — Afficher l'aide\n/settings — Paramètres"
            userMessage.contains("bonjour", ignoreCase = true) ->
                "Bonjour ! 😊 Comment puis-je vous aider aujourd'hui ?"
            userMessage.contains("merci", ignoreCase = true) ->
                "De rien ! N'hésitez pas si vous avez d'autres questions. 🙂"
            else ->
                "J'ai bien reçu votre message : \"$userMessage\"\n\nQue souhaitez-vous faire ?"
        }
    }

    private fun generateInlineKeyboard(userMessage: String): ReplyMarkup? {
        if (userMessage.equals("/start", ignoreCase = true)) {
            return ReplyMarkup(
                rows = listOf(
                    listOf(
                        InlineButton("📖 Aide", InlineButtonType.CALLBACK, "/help"),
                        InlineButton("⚙️ Paramètres", InlineButtonType.CALLBACK, "/settings")
                    ),
                    listOf(
                        InlineButton("🌐 Ouvrir Mini-App", InlineButtonType.WEB_APP, "https://example.com")
                    )
                )
            )
        }
        return null
    }

    private fun loadDemoMessages() {
        // Messages de bienvenue (comme le démarrage d'un bot Telegram)
        messages.add(
            Message(
                botId = botId,
                text = "👋 Bienvenue dans $botName !\n\nAppuyez sur /start pour commencer.",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 300_000,
                replyMarkup = ReplyMarkup(
                    rows = listOf(
                        listOf(
                            InlineButton("▶️ Démarrer", InlineButtonType.CALLBACK, "/start")
                        )
                    )
                )
            )
        )
        adapter.notifyDataSetChanged()
    }
}
