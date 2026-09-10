package com.botpro.app.data.fixtures

import com.botpro.app.data.model.*

/**
 * Fixtures rejouant exactement les 10 captures étalons analysées
 * dans reference/conversation/CORRESPONDANCES.md.
 * Chaque fixture permet de mesurer et valider un composant précis.
 */
object ConversationFixtures {

    val DEMO_BOT = Bot(
        id = "bot-moodwoop",
        name = "Moodwoop 🫥",
        username = "moodwoop_bot",
        description = "Assistant robotisé de trading et supervision BotPro.",
        isOnline = true,
        commands = listOf(
            BotCommand("/help", "Affiche la documentation d'aide"),
            BotCommand("/new", "Démarre une nouvelle session"),
            BotCommand("/stop", "Arrête le processus en cours"),
            BotCommand("/status", "Affiche l'état du serveur"),
            BotCommand("/resume", "Reprend les tâches en pause"),
            BotCommand("/sessions", "Liste des sessions actives"),
            BotCommand("/model", "Sélectionne le modèle d'inférence"),
            BotCommand("/debug", "Active ou désactive les logs détaillés"),
            BotCommand("/restart", "Redémarre le conteneur"),
            BotCommand("/update", "Met à jour le bot vers la dernière version")
        )
    )

    /**
     * Génère la liste complète des 15 fixtures pour validation écran.
     */
    fun createReplayMessages(botId: String = DEMO_BOT.id): List<Message> {
        val list = mutableListOf<Message>()
        val baseTime = 1720000000000L

        // 1. Séparateur de date (conv-01, conv-05) : pastille 30.9dp
        list.add(
            Message(
                id = "fix-date-1",
                botId = botId,
                text = "juin 26",
                isDateSeparator = true,
                type = MessageType.DATE_SEPARATOR
            )
        )

        // 2. Message court entrant (conv-07) : cellule 107px (38.9dp)
        list.add(
            Message(
                id = "fix-short-in",
                botId = botId,
                text = "Hello",
                isOutgoing = false,
                timestamp = baseTime + 60000,
                type = MessageType.TEXT
            )
        )

        // 3. Message court sortant (conv-07) : pointe de bulle débord 15dp
        list.add(
            Message(
                id = "fix-short-out",
                botId = botId,
                text = "Hmm",
                isOutgoing = true,
                timestamp = baseTime + 120000,
                isRead = true,
                type = MessageType.TEXT
            )
        )

        // 4. Salve de messages entrants groupés (conv-05) : angles 5dp
        list.add(
            Message(
                id = "fix-group-1",
                botId = botId,
                text = "Si si",
                isOutgoing = false,
                timestamp = baseTime + 180000,
                isGroupedTop = false,
                isGroupedBottom = true,
                type = MessageType.TEXT
            )
        )
        list.add(
            Message(
                id = "fix-group-2",
                botId = botId,
                text = "Donc je m'ennuie",
                isOutgoing = false,
                timestamp = baseTime + 240000,
                isGroupedTop = true,
                isGroupedBottom = false,
                type = MessageType.TEXT
            )
        )

        // 5. Message transféré avec photo/document (conv-01, conv-05)
        list.add(
            Message(
                id = "fix-fwd",
                botId = botId,
                text = "Voici les documents d'analyse financière.",
                isOutgoing = false,
                timestamp = baseTime + 300000,
                forwardInfo = MessageForwardInfo(authorName = "Audier"),
                type = MessageType.TEXT
            )
        )

        // 6. Document entrant avec nom et poids (conv-05)
        list.add(
            Message(
                id = "fix-doc-in",
                botId = botId,
                text = "Archives_2026.7z",
                isOutgoing = false,
                timestamp = baseTime + 360000,
                documentInfo = MessageDocumentInfo(
                    fileName = "Archives_2026.7z",
                    fileSizeString = "425,6 MB",
                    extension = "7z"
                ),
                type = MessageType.DOCUMENT
            )
        )

        // 7. Mention et commande cliquable dans le texte (conv-05, conv-08)
        list.add(
            Message(
                id = "fix-mention-cmd",
                botId = botId,
                text = "Contacte @TrvPunisher_bot ou tape /reset pour relancer.",
                isOutgoing = false,
                timestamp = baseTime + 420000,
                type = MessageType.TEXT
            )
        )

        // 8. Commande de bot envoyée avec accusé de lecture (conv-08)
        list.add(
            Message(
                id = "fix-cmd-stop",
                botId = botId,
                text = "/stop",
                isOutgoing = true,
                timestamp = baseTime + 480000,
                isRead = true,
                type = MessageType.BOT_COMMAND
            )
        )

        // 9. Message entrant avec citation / réponse (conv-08)
        list.add(
            Message(
                id = "fix-reply-quote",
                botId = botId,
                text = "gateway.stop.stopped\nProcessus suspendu avec succès.",
                isOutgoing = false,
                timestamp = baseTime + 540000,
                replyInfo = MessageReplyInfo(
                    authorName = "Moodwoop 🫥",
                    text = "/stop"
                ),
                type = MessageType.TEXT
            )
        )

        // 10. Commande /resume envoyée (conv-08)
        list.add(
            Message(
                id = "fix-cmd-resume",
                botId = botId,
                text = "/resume",
                isOutgoing = true,
                timestamp = baseTime + 600000,
                isRead = true,
                type = MessageType.BOT_COMMAND
            )
        )

        // 11. Message long multi-lignes avec citation (conv-02, conv-08)
        list.add(
            Message(
                id = "fix-multiline-quote",
                botId = botId,
                text = "gateway.resume.list_header\n• Service d'authentification prêt\n• Base de données synchronisée\n• Connecteur MTProto actif\n• Surveillance des alertes en cours\ngateway.resume.list_footer_numbered",
                isOutgoing = false,
                timestamp = baseTime + 660000,
                replyInfo = MessageReplyInfo(
                    authorName = "Moodwoop 🫥",
                    text = "/resume"
                ),
                type = MessageType.TEXT
            )
        )

        // 12. Message avec boutons inline peints au Canvas (Phase A1)
        list.add(
            Message(
                id = "fix-inline-buttons",
                botId = botId,
                text = "Voulez-vous valider l'exécution de la stratégie ?",
                isOutgoing = false,
                timestamp = baseTime + 720000,
                replyMarkup = ReplyMarkup(
                    rows = listOf(
                        listOf(
                            InlineButton(text = "✅ Confirmer", data = "cmd_confirm"),
                            InlineButton(text = "❌ Annuler", data = "cmd_cancel")
                        ),
                        listOf(
                            InlineButton(text = "📊 Détails complets", data = "cmd_details")
                        )
                    )
                ),
                type = MessageType.TEXT
            )
        )

        // 13. Document sortant avec accusé de lecture (conv-05)
        list.add(
            Message(
                id = "fix-doc-out",
                botId = botId,
                text = "rapport_mensuel.zip",
                isOutgoing = true,
                timestamp = baseTime + 780000,
                isRead = true,
                documentInfo = MessageDocumentInfo(
                    fileName = "rapport_mensuel.zip",
                    fileSizeString = "12,4 MB",
                    extension = "zip"
                ),
                type = MessageType.DOCUMENT
            )
        )

        // 14. Message sélectionné (conv-03) : fond bulle #314A61
        list.add(
            Message(
                id = "fix-selected",
                botId = botId,
                text = "Message entrant sélectionné pour démonstration du mode sélection.",
                isOutgoing = false,
                timestamp = baseTime + 840000,
                isSelected = false,
                type = MessageType.TEXT
            )
        )

        return list
    }
}
