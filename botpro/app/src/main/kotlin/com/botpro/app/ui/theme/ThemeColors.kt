package com.botpro.app.ui.theme

/**
 * Couleurs résolues du thème Dark Blue de Telegram pour l'écran Dialogs.
 * Directement extraites et mesurées depuis la source Telegram et les captures étalons.
 * Règle d'or : aucune valeur visuelle n'est inventée ou estimée.
 */
object ThemeColors {

    // Fond & Structure
    const val WINDOW_BACKGROUND_WHITE = 0xFF1D2733.toInt() // #1D2733 (fond liste)
    const val WINDOW_BACKGROUND_GRAY = 0xFF151E27.toInt()
    const val DIVIDER = 0x95000000.toInt() // #000000 avec alpha 149 (0x95)
    const val LIST_SELECTOR_SDK21 = 0x14E6F7FF.toInt() // #E6F7FF avec alpha 20 (0x14)

    // Barre du haut (Action Bar)
    const val ACTION_BAR_DEFAULT = 0xFF1D2733.toInt() // #1D2733 (fond fondu avec la liste, DialogsActivity.java:3502)
    const val ACTION_BAR_DEFAULT_TITLE = 0xFFFFFFFF.toInt()
    const val ACTION_BAR_DEFAULT_ICON = 0xFFFFFFFF.toInt()
    const val ACTION_BAR_DEFAULT_SELECTOR = 0x1ECFE8FF.toInt() // #CFE8FF avec alpha 30
    const val ACTION_BAR_DEFAULT_SEARCH = 0xFFFFFFFF.toInt()
    const val ACTION_BAR_DEFAULT_SEARCH_PLACEHOLDER = 0x78DCF4FF.toInt() // #DCF4FF avec alpha 120

    // Textes de cellule
    const val CHATS_NAME = 0xFFE9EEF4.toInt() // #E9EEF4 (nom)
    const val CHATS_NAME_MESSAGE = 0xFFE9EEF4.toInt()
    const val CHATS_MESSAGE = 0xFF7D8B99.toInt() // #7D8B99 (aperçu)
    const val CHATS_DRAFT = 0xD9FC474A.toInt() // #FC474A avec alpha 217
    const val CHATS_DATE = 0xFF737F8B.toInt() // #737F8B (heure)
    const val CHATS_ATTACH_MESSAGE = 0xFF7D8E98.toInt()

    // Badges & Icônes
    const val CHATS_UNREAD_COUNTER = 0xFF64B5EF.toInt() // #64B5EF (badge non lu actif)
    const val CHATS_UNREAD_COUNTER_MUTED = 0xFF3E5263.toInt() // #3E5263 (badge silencieux)
    const val CHATS_UNREAD_COUNTER_TEXT = 0xFFFFFFFF.toInt()
    const val CHATS_MUTE_ICON = 0xFF4E5F6A.toInt()
    const val CHATS_PINNED_ICON = 0xFF586D80.toInt()
    const val CHATS_PINNED_OVERLAY = 0x08FFFFFF.toInt() // #FFFFFF avec alpha 8
    const val CHATS_VERIFIED_BACKGROUND = 0xFF64B5EF.toInt()
    const val CHATS_VERIFIED_CHECK = 0xFFFFFFFF.toInt()
    const val CHATS_SENT_CHECK = 0xFF64B5EF.toInt()
    const val CHATS_SENT_READ_CHECK = 0xFF46AA36.toInt()
    const val CHATS_SENT_CLOCK = 0xFF4C5F6A.toInt()
    const val CHATS_ONLINE_CIRCLE = 0xFF4BCB1C.toInt()
    const val CHATS_SECRET_NAME = 0xFF71D756.toInt()

    // Bouton d'action flottant (FAB)
    const val CHATS_ACTION_BACKGROUND = 0xFF5FA3DE.toInt()
    const val CHATS_ACTION_PRESSED_BACKGROUND = 0xFF569DD6.toInt()
    const val CHATS_ACTION_ICON = 0xFFFFFFFF.toInt()

    // Avatars sans photo (palette Telegram)
    const val AVATAR_BACKGROUND_BLUE = 0xFF5CAFFA.toInt()
    const val AVATAR_BACKGROUND_RED = 0xFFE86C6A.toInt()
    const val AVATAR_BACKGROUND_ORANGE = 0xFFF2BC64.toInt()
    const val AVATAR_BACKGROUND_VIOLET = 0xFFB694F9.toInt()
    const val AVATAR_BACKGROUND_GREEN = 0xFF9AD164.toInt()
    const val AVATAR_BACKGROUND_CYAN = 0xFF5BCBE3.toInt()
    const val AVATAR_BACKGROUND_PINK = 0xFFFF8AAC.toInt()
    const val AVATAR_TEXT = 0xFFFFFFFF.toInt()

    val AVATAR_COLORS = intArrayOf(
        AVATAR_BACKGROUND_RED,
        AVATAR_BACKGROUND_ORANGE,
        AVATAR_BACKGROUND_VIOLET,
        AVATAR_BACKGROUND_GREEN,
        AVATAR_BACKGROUND_CYAN,
        AVATAR_BACKGROUND_BLUE,
        AVATAR_BACKGROUND_PINK
    )

    fun getAvatarColor(id: String): Int {
        val hash = kotlin.math.abs(id.hashCode())
        return AVATAR_COLORS[hash % AVATAR_COLORS.size]
    }

    // ═══ Écran de Conversation (Dark Blue Theme & Mesures étalons) ═══

    // Fond de conversation
    const val CHAT_WALLPAPER = 0xFF151E27.toInt() // #151E27

    // Bulle entrante
    const val CHAT_IN_BUBBLE = 0xFF232E3B.toInt() // #232E3B (conv-02)
    const val CHAT_IN_BUBBLE_SELECTED = 0xFF314A61.toInt() // #314A61 (conv-03)
    const val CHAT_MESSAGE_TEXT_IN = 0xFFFFFFFF.toInt() // #FFFFFF
    const val CHAT_IN_TIME_TEXT = 0xFF708499.toInt() // #708499
    const val CHAT_IN_REPLY_LINE = 0xFF7DD071.toInt() // #7DD071 barre 3.3dp (conv-08)
    const val CHAT_IN_REPLY_BACKGROUND = 0xFF2C3D40.toInt() // #2C3D40 (conv-08)
    const val CHAT_IN_REPLY_NAME = 0xFF7DD071.toInt() // #7DD071
    const val CHAT_IN_LINKS = 0xFF64B5EF.toInt() // #64B5EF (commandes & mentions bleues)

    // Bulle sortante (Dégradé vertical ancré à l'écran, Theme.java:1776-1780)
    const val CHAT_OUT_BUBBLE_GRADIENT_3 = 0xFF9F3EAA.toInt() // #9F3EAA (haut, y=0)
    const val CHAT_OUT_BUBBLE_GRADIENT_2 = 0xFF8146D7.toInt() // #8146D7 (1/3)
    const val CHAT_OUT_BUBBLE_GRADIENT_1 = 0xFF4272DF.toInt() // #4272DF (2/3)
    const val CHAT_OUT_BUBBLE = 0xFF258DE5.toInt() // #258DE5 (bas, y=screenHeight)
    const val CHAT_MESSAGE_TEXT_OUT = 0xFFFFFFFF.toInt() // #FFFFFF
    const val CHAT_OUT_TIME_TEXT = 0x8AFFFFFF.toInt() // Blanc alpha 54%
    const val CHAT_OUT_SENT_CHECK = 0xFF87C7FF.toInt() // #87C7FF (double coche, conv-05)
    const val CHAT_OUT_REPLY_LINE = 0xFFFFFFFF.toInt()
    const val CHAT_OUT_REPLY_BACKGROUND = 0x26000000.toInt() // Noir alpha 15%

    // Séparateur de date & pastilles d'action
    const val CHAT_SERVICE_BACKGROUND = 0x66000000.toInt() // #000000 alpha 40% (0x66)
    const val CHAT_SERVICE_TEXT = 0xFFFFFFFF.toInt() // #FFFFFF

    // Boutons inline de bot (peints dans la bulle, Phase A1)
    const val BOT_INLINE_BUTTON_BACKGROUND = 0x66000000.toInt() // #000000 alpha 40%
    const val BOT_INLINE_BUTTON_TEXT = 0xFFFFFFFF.toInt() // #FFFFFF

    // Clavier de réponse de bot (vraies vues, conv-09)
    const val BOT_KEYBOARD_BACKGROUND = 0xFF212D3B.toInt() // #212D3B (fond panneau)
    const val BOT_KEYBOARD_BUTTON = 0xFF293645.toInt() // #293645 (fond bouton)
    const val BOT_KEYBOARD_BUTTON_TEXT = 0xFFFFFFFF.toInt()

    // Menu de commandes de bot (conv-08 & conv-10)
    const val BOT_MENU_BUTTON_BACKGROUND = 0xFF229AF0.toInt() // #229AF0 (bouton ☰ Menu, 82.5dp)
    const val BOT_MENU_SHEET_BACKGROUND = 0xFF1D2733.toInt() // #1D2733 (panneau commandes)
    const val BOT_MENU_HANDLE = 0xFF3E4651.toInt() // #3E4651 (poignée 21.8dp)
    const val BOT_MENU_TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    const val BOT_MENU_TEXT_COMMAND = 0xFF7D8B99.toInt() // #7D8B99

    // Barre de saisie
    const val CHAT_INPUT_BACKGROUND = 0xFF1D2733.toInt() // #1D2733
    const val CHAT_INPUT_FIELD_BACKGROUND = 0xFF242F3D.toInt() // #242F3D
    const val CHAT_INPUT_ICONS = 0xFF7D8B99.toInt() // #7D8B99
    const val CHAT_INPUT_HINT = 0xFF7D8B99.toInt()
}
