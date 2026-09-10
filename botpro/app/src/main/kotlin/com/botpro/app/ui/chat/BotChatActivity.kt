package com.botpro.app.ui.chat

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpro.app.R
import com.botpro.app.core.utils.AndroidUtilities
import com.botpro.app.core.utils.AndroidUtilities.dp
import com.botpro.app.data.fixtures.ConversationFixtures
import com.botpro.app.data.model.*
import com.botpro.app.ui.theme.ThemeColors

/**
 * Écran de conversation avec un bot, reproduction exacte de ChatActivity de Telegram.
 * Conforme aux 10 captures étalons (reference/conversation/CORRESPONDANCES.md) :
 * - Barre du haut flottante au-dessus de la liste (conv-04).
 * - Insets bord-à-bord targetSdk 35 appliqués dynamiquement.
 * - Barre de saisie adaptative à 3 états (conv-01, conv-08, conv-09).
 * - Clavier de réponse en vraies vues (conv-09).
 * - Menu coulissant de commandes de bot (conv-10).
 * - Mode sélection multiple (conv-03).
 */
class BotChatActivity : AppCompatActivity() {

    private lateinit var rootLayout: ViewGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var headerContainer: FrameLayout
    private lateinit var toolbarContent: ViewGroup
    private lateinit var selectionToolbar: ViewGroup
    private lateinit var toolbarTitle: TextView
    private lateinit var toolbarSubtitle: TextView
    private lateinit var toolbarAvatarBg: View
    private lateinit var toolbarAvatarText: TextView
    private lateinit var backButton: ImageView

    private lateinit var bottomContainer: ViewGroup
    private lateinit var inputContainer: ViewGroup
    private lateinit var inputField: EditText
    private lateinit var btnBotMenu: TextView
    private lateinit var btnEmoji: ImageView
    private lateinit var btnBotKeyboardToggle: ImageView
    private lateinit var btnAttach: ImageView
    private lateinit var btnSendOrMic: ImageView

    private lateinit var botCommandMenu: BotCommandMenuView
    private lateinit var botKeyboardView: BotKeyboardView
    private lateinit var selectionBottomContainer: ViewGroup

    private lateinit var adapter: ChatMessagesAdapter
    private val messages = mutableListOf<Message>()

    private var botId: String = ""
    private var botName: String = ""
    private var botUsername: String = ""
    private var isBot: Boolean = true

    // États de l'interface
    private var isKeyboardOpen: Boolean = false
    private var isMenuOpen: Boolean = false
    private var isSelectionMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = ThemeColors.CHAT_INPUT_BACKGROUND

        setContentView(R.layout.activity_bot_chat)

        // Données du bot
        botId = intent.getStringExtra("bot_id") ?: ConversationFixtures.DEMO_BOT.id
        botName = intent.getStringExtra("bot_name") ?: ConversationFixtures.DEMO_BOT.name
        botUsername = intent.getStringExtra("bot_username") ?: ConversationFixtures.DEMO_BOT.username
        isBot = intent.getBooleanExtra("is_bot", true)

        initViews()
        setupInsets()
        setupToolbar()
        setupChat()
        setupBottomBar()
        loadFixtures()
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.chat_root_layout)
        recyclerView = findViewById(R.id.chat_recycler_view)
        headerContainer = findViewById(R.id.chat_header_container)
        toolbarContent = findViewById(R.id.chat_toolbar_content)
        selectionToolbar = findViewById(R.id.chat_selection_toolbar)
        toolbarTitle = findViewById(R.id.toolbar_title)
        toolbarSubtitle = findViewById(R.id.toolbar_subtitle)
        toolbarAvatarBg = findViewById(R.id.toolbar_avatar_bg)
        toolbarAvatarText = findViewById(R.id.toolbar_avatar_text)
        backButton = findViewById(R.id.btn_back)

        bottomContainer = findViewById(R.id.chat_bottom_container)
        inputContainer = findViewById(R.id.chat_input_container)
        inputField = findViewById(R.id.chat_input_field)
        btnBotMenu = findViewById(R.id.btn_bot_menu)
        btnEmoji = findViewById(R.id.btn_emoji)
        btnBotKeyboardToggle = findViewById(R.id.btn_bot_keyboard_toggle)
        btnAttach = findViewById(R.id.btn_attach)
        btnSendOrMic = findViewById(R.id.btn_send_or_mic)

        botCommandMenu = findViewById(R.id.bot_command_menu)
        botKeyboardView = findViewById(R.id.bot_keyboard_view)
        selectionBottomContainer = findViewById(R.id.selection_bottom_container)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // 1. L'en-tête flottant reçoit le padding de la barre d'état
            headerContainer.setPadding(0, statusBars.top, 0, 0)

            // 2. La liste défile SOUS l'en-tête : padding haut = statusBars.top + 56dp
            val headerH = statusBars.top + dp(56f)
            recyclerView.setPadding(0, headerH, 0, dp(8f))

            // 3. Le conteneur bas reçoit le padding de navigation
            bottomContainer.setPadding(0, 0, 0, navBars.bottom)

            insets
        }
    }

    private fun setupToolbar() {
        toolbarTitle.text = botName
        toolbarSubtitle.text = if (isBot) "bot" else "last seen recently"

        // Avatar
        val initial = if (botName.isNotEmpty()) botName.first().uppercase() else "B"
        toolbarAvatarText.text = initial
        val avatarBgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ThemeColors.getAvatarColor(botId))
        }
        toolbarAvatarBg.background = avatarBgDrawable

        backButton.setOnClickListener { finish() }

        // Mode sélection : bouton fermer
        findViewById<View>(R.id.btn_close_selection)?.setOnClickListener {
            exitSelectionMode()
        }
    }

    private fun setupChat() {
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.layoutManager = layoutManager

        adapter = ChatMessagesAdapter(messages) { button ->
            when (button.type) {
                InlineButtonType.CALLBACK -> {
                    button.data?.let { sendUserMessage(it) }
                }
                InlineButtonType.WEB_APP -> {
                    val intent = Intent(this, com.botpro.app.ui.botwebview.BotWebViewActivity::class.java).apply {
                        putExtra("url", button.url ?: "https://telegram.org")
                        putExtra("title", button.text)
                    }
                    startActivity(intent)
                }
                InlineButtonType.URL -> {
                    button.url?.let { url ->
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(browserIntent)
                    }
                }
                InlineButtonType.SWITCH_INLINE -> {}
            }
        }
        recyclerView.adapter = adapter
    }

    private fun setupBottomBar() {
        // Forme arrondie du champ de saisie
        val inputBgDrawable = GradientDrawable().apply {
            setColor(ThemeColors.CHAT_INPUT_FIELD_BACKGROUND)
            cornerRadius = dp(20f).toFloat()
        }
        inputField.background = inputBgDrawable

        // Forme arrondie du bouton Menu
        updateMenuButtonAppearance()

        // Configuration du clavier de réponse (conv-09)
        botKeyboardView.setButtons(listOf(listOf("🚨 ALERT", "📡 SIGNAL", "📊 Monitoring")))
        botKeyboardView.onButtonClickListener = { label ->
            sendUserMessage(label)
        }

        // Configuration du menu de commandes (conv-10)
        botCommandMenu.setCommands(ConversationFixtures.DEMO_BOT.commands)
        botCommandMenu.onCommandSelected = { cmd ->
            isMenuOpen = false
            botCommandMenu.visibility = View.GONE
            updateInputBarState()
            sendUserMessage(cmd)
        }

        // Bascule Menu de commandes
        btnBotMenu.setOnClickListener {
            isMenuOpen = !isMenuOpen
            if (isMenuOpen) {
                isKeyboardOpen = false
                botKeyboardView.visibility = View.GONE
                botCommandMenu.visibility = View.VISIBLE
            } else {
                botCommandMenu.visibility = View.GONE
            }
            updateInputBarState()
        }

        // Bascule Clavier de bot
        btnBotKeyboardToggle.setOnClickListener {
            isKeyboardOpen = !isKeyboardOpen
            if (isKeyboardOpen) {
                isMenuOpen = false
                botCommandMenu.visibility = View.GONE
                botKeyboardView.visibility = View.VISIBLE
            } else {
                botKeyboardView.visibility = View.GONE
            }
            updateInputBarState()
        }

        // Détection de frappe pour transformer le bouton mic en bouton envoi
        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                btnSendOrMic.setImageResource(
                    if (hasText) android.R.drawable.ic_menu_send
                    else android.R.drawable.ic_btn_speak_now
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Envoi
        btnSendOrMic.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                sendUserMessage(text)
                inputField.text.clear()
            }
        }

        updateInputBarState()
    }

    private fun updateInputBarState() {
        if (!isBot) {
            btnBotMenu.visibility = View.GONE
            btnBotKeyboardToggle.visibility = View.GONE
            return
        }

        btnBotMenu.visibility = View.VISIBLE
        btnBotKeyboardToggle.visibility = View.VISIBLE

        updateMenuButtonAppearance()
    }

    private fun updateMenuButtonAppearance() {
        val menuDrawable = GradientDrawable().apply {
            setColor(ThemeColors.BOT_MENU_BUTTON_BACKGROUND)
            if (isKeyboardOpen) {
                // conv-09 : icône seule, bouton rond 44dp
                shape = GradientDrawable.OVAL
            } else {
                // conv-08 & conv-10 : rectangle arrondi 82.5dp
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(19f).toFloat()
            }
        }
        btnBotMenu.background = menuDrawable

        val lp = btnBotMenu.layoutParams as LinearLayout.LayoutParams
        if (isKeyboardOpen) {
            lp.width = dp(44f)
            btnBotMenu.text = "☰"
        } else if (isMenuOpen) {
            lp.width = dp(82.5f)
            btnBotMenu.text = "✕ Menu"
        } else {
            lp.width = dp(82.5f)
            btnBotMenu.text = "☰ Menu"
        }
        btnBotMenu.layoutParams = lp
    }

    private fun loadFixtures() {
        messages.clear()
        messages.addAll(ConversationFixtures.createReplayMessages(botId))
        adapter.notifyDataSetChanged()
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun sendUserMessage(text: String) {
        val outMessage = Message(
            botId = botId,
            text = text,
            isOutgoing = true,
            timestamp = System.currentTimeMillis(),
            isRead = true,
            type = if (text.startsWith("/")) MessageType.BOT_COMMAND else MessageType.TEXT
        )
        messages.add(outMessage)
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)

        // Réponse simulée après court délai
        recyclerView.postDelayed({
            val botReply = Message(
                botId = botId,
                text = "Exécution de $text confirmée par le serveur.",
                isOutgoing = false,
                timestamp = System.currentTimeMillis(),
                replyInfo = MessageReplyInfo(authorName = "Moi", text = text),
                type = MessageType.TEXT
            )
            messages.add(botReply)
            adapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }, 600)
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        toolbarContent.visibility = View.GONE
        selectionToolbar.visibility = View.VISIBLE
        inputContainer.visibility = View.GONE
        selectionBottomContainer.visibility = View.VISIBLE
        botKeyboardView.visibility = View.GONE
        botCommandMenu.visibility = View.GONE
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        toolbarContent.visibility = View.VISIBLE
        selectionToolbar.visibility = View.GONE
        inputContainer.visibility = View.VISIBLE
        selectionBottomContainer.visibility = View.GONE
    }
}
