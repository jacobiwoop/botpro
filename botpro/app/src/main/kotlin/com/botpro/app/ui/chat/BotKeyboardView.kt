package com.botpro.app.ui.chat

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.botpro.app.core.utils.AndroidUtilities.dp
import com.botpro.app.core.utils.TypefaceManager
import com.botpro.app.ui.theme.ThemeColors

/**
 * Clavier de réponse de bot (Bottom Response Keyboard),
 * reproduisant fidèlement les mesures de conv-09 :
 * - Vraies vues (LinearLayout / TextView)
 * - Hauteur d'un bouton : 44.0dp (121px @440dpi)
 * - Fond bouton : #293645
 * - Écart entre boutons : 4dp (11px @440dpi)
 * - Marges latérales panneau : 8dp (22px @440dpi)
 * - Fond panneau : #212D3B
 */
class BotKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var onButtonClickListener: ((String) -> Unit)? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(ThemeColors.BOT_KEYBOARD_BACKGROUND)
        val padH = dp(8f)
        val padV = dp(6f)
        setPadding(padH, padV, padH, padV)
    }

    /**
     * Remplit le clavier avec les lignes de boutons.
     * Exemple conv-09 : ["🚨 ALERT", "📡 SIGNAL", "📊 Monitoring"]
     */
    fun setButtons(rows: List<List<String>>) {
        removeAllViews()
        val btnH = dp(44f)
        val gap = dp(4f)
        val cornerRadius = dp(6f).toFloat()

        for (rowIndex in rows.indices) {
            val row = rows[rowIndex]
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    btnH
                ).apply {
                    if (rowIndex > 0) topMargin = gap
                }
            }

            for (colIndex in row.indices) {
                val label = row[colIndex]
                val bgDrawable = GradientDrawable().apply {
                    setColor(ThemeColors.BOT_KEYBOARD_BUTTON)
                    setCornerRadius(dp(6f).toFloat())
                }

                val textView = TextView(context).apply {
                    text = label
                    setTextColor(ThemeColors.BOT_KEYBOARD_BUTTON_TEXT)
                    textSize = 14f
                    typeface = TypefaceManager.getMedium(context)
                    gravity = Gravity.CENTER
                    background = bgDrawable
                    isClickable = true
                    isFocusable = true
                    layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                        if (colIndex > 0) marginStart = gap
                    }
                    setOnClickListener {
                        onButtonClickListener?.invoke(label)
                    }
                }
                rowLayout.addView(textView)
            }
            addView(rowLayout)
        }
    }
}
