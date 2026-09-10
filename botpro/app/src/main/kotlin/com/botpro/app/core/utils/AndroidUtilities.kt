package com.botpro.app.core.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.util.TypedValue
import android.view.WindowManager
import com.botpro.app.BotProApplication
import kotlin.math.ceil

/**
 * Utilitaires Android adaptés de Telegram `AndroidUtilities.java`.
 * Fournit les fonctions dp(), les dimensions d'écran, etc.
 */
object AndroidUtilities {

    var density: Float = 1f
        private set
    var displaySize = Point()
        private set
    var statusBarHeight: Int = 0
        private set

    fun init(context: Context) {
        density = context.resources.displayMetrics.density
        checkDisplaySize(context)
        statusBarHeight = getStatusBarHeightInternal(context)
    }

    /**
     * Convertit des dp en pixels (équivalent exact de Telegram `AndroidUtilities.dp()`).
     */
    fun dp(value: Float): Int {
        return if (value == 0f) 0
        else ceil((density * value).toDouble()).toInt()
    }

    fun dp(value: Int): Int = dp(value.toFloat())

    /**
     * Convertit des dp en float pixels.
     */
    fun dpf2(value: Float): Float {
        return if (value == 0f) 0f
        else density * value
    }

    /**
     * Interpole linéairement entre deux valeurs.
     */
    fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    fun lerp(a: Int, b: Int, t: Float): Int {
        return (a + (b - a) * t).toInt()
    }

    /**
     * Vérifie et met à jour les dimensions de l'écran.
     */
    fun checkDisplaySize(context: Context) {
        try {
            val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = manager.defaultDisplay
            display?.getSize(displaySize)
        } catch (_: Exception) {
            // Fallback
        }
    }

    private fun getStatusBarHeightInternal(context: Context): Int {
        val resourceId = context.resources.getIdentifier(
            "status_bar_height", "dimen", "android"
        )
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            dp(25)
        }
    }

    /**
     * Retourne la hauteur de la barre de navigation.
     */
    fun getNavigationBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier(
            "navigation_bar_height", "dimen", "android"
        )
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    /**
     * Vérifie si le thème courant est un thème sombre.
     */
    fun isDarkTheme(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
