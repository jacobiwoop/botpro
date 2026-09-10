package com.botpro.app.core.utils

import android.content.Context
import android.graphics.Typeface

/**
 * Gestionnaire de polices Telegram avec cache.
 * Charge les typographies depuis les assets et évite toute réallocation en cours de dessin.
 */
object TypefaceManager {

    private var medium: Typeface? = null
    private var italic: Typeface? = null
    private var bold: Typeface? = null
    private var mono: Typeface? = null

    fun getMedium(context: Context): Typeface {
        if (medium == null) {
            medium = Typeface.createFromAsset(context.assets, "fonts/rmedium.ttf")
        }
        return medium ?: Typeface.DEFAULT
    }

    fun getItalic(context: Context): Typeface {
        if (italic == null) {
            italic = Typeface.createFromAsset(context.assets, "fonts/ritalic.ttf")
        }
        return italic ?: Typeface.DEFAULT
    }

    fun getBold(context: Context): Typeface {
        if (bold == null) {
            bold = Typeface.createFromAsset(context.assets, "fonts/rextrabold.ttf")
        }
        return bold ?: Typeface.DEFAULT
    }

    fun getMono(context: Context): Typeface {
        if (mono == null) {
            mono = Typeface.createFromAsset(context.assets, "fonts/rmono.ttf")
        }
        return mono ?: Typeface.MONOSPACE
    }
}
