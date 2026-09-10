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
    const val ACTION_BAR_DEFAULT = 0xFF242D39.toInt() // #242D39 (fond)
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
}
