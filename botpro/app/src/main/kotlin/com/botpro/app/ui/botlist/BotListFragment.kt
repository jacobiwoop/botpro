package com.botpro.app.ui.botlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpro.app.R
import com.botpro.app.core.utils.AndroidUtilities
import com.botpro.app.core.utils.TypefaceManager
import com.botpro.app.data.fixtures.ConversationFixtures
import com.botpro.app.data.model.Bot
import com.botpro.app.data.model.BotCommand
import com.botpro.app.data.model.Conversation
import com.botpro.app.data.model.Message
import com.botpro.app.ui.chat.BotChatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.RelativeCornerSize

/**
 * Fragment affichant la liste des conversations avec les bots.
 * Conforme à Telegram Dialogs :
 * - Gestion des insets bord à bord (Android 15 / targetSdk 35)
 * - En-tête sans démarcation (#1D2733)
 * - FAB en cercle parfait (56dp)
 */
class BotListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: BotListAdapter

    // Données de démonstration réalistes pour valider les mesures
    private val demoConversations = listOf(
        Conversation(
            bot = ConversationFixtures.DEMO_BOT,
            lastMessage = Message(
                botId = ConversationFixtures.DEMO_BOT.id,
                text = "gateway.stop.stopped",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 30_000
            ),
            unreadCount = 1
        ),
        Conversation(
            bot = Bot(
                id = "1",
                name = "Assistant IA",
                username = "assistant_bot",
                description = "Un assistant intelligent qui répond à toutes vos questions",
                commands = listOf(BotCommand("/start", "Démarrer"), BotCommand("/help", "Aide")),
                isOnline = true
            ),
            lastMessage = Message(
                botId = "1",
                text = "Bonjour ! Comment puis-je vous aider ?",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 60_000
            ),
            unreadCount = 2
        ),
        Conversation(
            bot = Bot(
                id = "2",
                name = "Traducteur Universel",
                username = "translator_bot",
                description = "Traduction instantanée multilingue",
                isOnline = false
            ),
            lastMessage = Message(
                botId = "2",
                text = "Envoyez-moi un texte à traduire",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 3600_000
            ),
            unreadCount = 0
        ),
        Conversation(
            bot = Bot(
                id = "3",
                name = "Météo Pro",
                username = "weather_bot",
                description = "Prévisions météo mondiales",
                isOnline = false
            ),
            lastMessage = Message(
                botId = "3",
                text = "Paris : 22°C, Ensoleillé ☀️",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 7200_000
            ),
            unreadCount = 0
        ),
        Conversation(
            bot = Bot(
                id = "4",
                name = "Rappels & Tâches",
                username = "reminder_bot",
                description = "Gestionnaire de rappels",
                isOnline = true
            ),
            lastMessage = Message(
                botId = "4",
                text = "⏰ Rappel : Réunion dans 30 minutes",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 14400_000
            ),
            unreadCount = 1
        ),
        // Cellule 4 : cible de mesure du script tools/measure_cell.py
        Conversation(
            bot = Bot(
                id = "5",
                name = "BotPro Notifications",
                username = "botpro_notifications_bot",
                description = "Notifications officielles BotPro",
                isOnline = true
            ),
            lastMessage = Message(
                botId = "5",
                text = "Code de connexion web. Ne le partagez jamais.",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 28800_000
            ),
            unreadCount = 1
        ),
        Conversation(
            bot = Bot(
                id = "6",
                name = "Générateur d'Images",
                username = "image_gen_bot",
                description = "Création d'images IA",
                isOnline = false
            ),
            lastMessage = Message(
                botId = "6",
                text = "Votre image haute résolution est prête !",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 86400_000
            ),
            unreadCount = 0
        ),
        Conversation(
            bot = Bot(
                id = "7",
                name = "Calculateur Financier",
                username = "finance_bot",
                description = "Suivi des devises et cryptos",
                isOnline = true
            ),
            lastMessage = Message(
                botId = "7",
                text = "BTC : 89 450 $ (+3.2%)",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 172800_000
            ),
            unreadCount = 5
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_bot_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Titre de l'ActionBar en police rmedium.ttf
        view.findViewById<TextView>(R.id.toolbar_title)?.apply {
            typeface = TypefaceManager.getMedium(requireContext())
        }

        // 2. FAB : forcer un cercle parfait
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_new_chat)
        fab?.shapeAppearanceModel = fab.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(RelativeCornerSize(0.5f))
            .build()

        // 3. Gestion des insets bord à bord (WindowInsetsCompat)
        val headerView = view.findViewById<View>(R.id.header_view)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyView = view.findViewById(R.id.empty_view)

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            headerView?.updatePadding(top = bars.top)
            recyclerView.updatePadding(bottom = bars.bottom)
            fab?.let { f ->
                val lp = f.layoutParams as? ViewGroup.MarginLayoutParams
                if (lp != null) {
                    lp.bottomMargin = bars.bottom + AndroidUtilities.dp(16f)
                    f.layoutParams = lp
                }
            }
            insets
        }

        // 4. RecyclerView setup
        adapter = BotListAdapter(demoConversations) { conversation ->
            val intent = Intent(requireContext(), BotChatActivity::class.java).apply {
                putExtra("bot_id", conversation.bot.id)
                putExtra("bot_name", conversation.bot.name)
                putExtra("bot_username", conversation.bot.username)
                putExtra("bot_online", conversation.bot.isOnline)
            }
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        if (demoConversations.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
}
