package com.botpro.app.ui.botlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.botpro.app.R
import com.botpro.app.data.model.Bot
import com.botpro.app.data.model.BotCommand
import com.botpro.app.data.model.Conversation
import com.botpro.app.data.model.Message
import com.botpro.app.data.model.MessageType
import com.botpro.app.ui.chat.BotChatActivity

/**
 * Fragment affichant la liste des conversations avec les bots.
 * Basé sur `DialogsActivity` de Telegram (14 435 lignes Java),
 * réécrit en Kotlin avec les mêmes patterns visuels :
 * - Avatar rond à gauche
 * - Nom du bot + dernier message
 * - Badge de messages non lus
 * - Heure du dernier message à droite
 *
 * Correspond visuellement à l'écran principal de Telegram
 * (liste des conversations), mais n'affiche que des bots.
 */
class BotListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: BotListAdapter

    // Données de démonstration (à remplacer par le repository)
    private val demoConversations = listOf(
        Conversation(
            bot = Bot(
                id = "1",
                name = "Assistant IA",
                username = "assistant_bot",
                description = "Un assistant intelligent qui répond à toutes vos questions",
                commands = listOf(
                    BotCommand("/start", "Démarrer la conversation"),
                    BotCommand("/help", "Afficher l'aide"),
                    BotCommand("/settings", "Paramètres du bot")
                ),
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
                name = "Traducteur",
                username = "translator_bot",
                description = "Traduit instantanément dans plus de 50 langues",
                commands = listOf(
                    BotCommand("/start", "Démarrer"),
                    BotCommand("/lang", "Choisir la langue"),
                    BotCommand("/detect", "Détecter la langue")
                ),
                isOnline = true
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
                description = "Prévisions météo précises pour toutes les villes du monde",
                commands = listOf(
                    BotCommand("/start", "Démarrer"),
                    BotCommand("/now", "Météo actuelle"),
                    BotCommand("/forecast", "Prévisions 7 jours")
                ),
                isOnline = false
            ),
            lastMessage = Message(
                botId = "3",
                text = "Paris : 22°C, Ensoleillé ☀️",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 86400_000
            ),
            unreadCount = 0
        ),
        Conversation(
            bot = Bot(
                id = "4",
                name = "Rappels",
                username = "reminder_bot",
                description = "Planifiez vos rappels et ne manquez plus rien",
                commands = listOf(
                    BotCommand("/start", "Démarrer"),
                    BotCommand("/new", "Nouveau rappel"),
                    BotCommand("/list", "Liste des rappels")
                ),
                isOnline = true
            ),
            lastMessage = Message(
                botId = "4",
                text = "⏰ Rappel : Réunion dans 30 minutes",
                isOutgoing = false,
                timestamp = System.currentTimeMillis() - 1800_000
            ),
            unreadCount = 1
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

        // Action bar title
        view.findViewById<TextView>(R.id.toolbar_title)?.text = getString(R.string.bot_list_title)

        // RecyclerView setup (comme DialogsActivity)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyView = view.findViewById(R.id.empty_view)

        adapter = BotListAdapter(demoConversations) { conversation ->
            // Navigation vers le chat (comme DialogsActivity -> ChatActivity)
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

        // Toggle empty state
        if (demoConversations.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
}
