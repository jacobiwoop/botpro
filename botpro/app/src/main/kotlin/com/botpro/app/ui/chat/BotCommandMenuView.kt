package com.botpro.app.ui.chat

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.botpro.app.core.utils.AndroidUtilities.dp
import com.botpro.app.core.utils.TypefaceManager
import com.botpro.app.data.model.BotCommand
import com.botpro.app.ui.theme.ThemeColors

/**
 * Menu de commandes de bot (conv-10) :
 * - Fond : #1D2733
 * - Marges latérales : 5.1dp (14px @440dpi)
 * - Poignée de glissement : 21.8dp de large, #3E4651, centrée
 * - Lignes de commande : 37.5dp (1 ligne) ou 56.4dp (2 lignes)
 * - Description à gauche (16dp marge) · /commande à droite (16dp marge)
 */
class BotCommandMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onCommandSelected: ((String) -> Unit)? = null

    private val container: LinearLayout

    init {
        val bgDrawable = GradientDrawable().apply {
            setColor(ThemeColors.BOT_MENU_SHEET_BACKGROUND)
            cornerRadii = floatArrayOf(
                dp(12f).toFloat(), dp(12f).toFloat(),
                dp(12f).toFloat(), dp(12f).toFloat(),
                0f, 0f, 0f, 0f
            )
        }
        background = bgDrawable

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // Poignée de glissement (21.8dp, #3E4651)
        val handle = View(context).apply {
            val handleDrawable = GradientDrawable().apply {
                setColor(ThemeColors.BOT_MENU_HANDLE)
                cornerRadius = dp(2f).toFloat()
            }
            background = handleDrawable
            val handleW = dp(21.8f)
            val handleH = dp(4f)
            layoutParams = LinearLayout.LayoutParams(handleW, handleH).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8f)
                bottomMargin = dp(8f)
            }
        }
        rootLayout.addView(handle)

        // Scroll view contenant la liste des commandes
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(280f) // hauteur max du panneau
            )
        }

        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        scrollView.addView(container)
        rootLayout.addView(scrollView)
        addView(rootLayout)
    }

    fun setCommands(commands: List<BotCommand>) {
        container.removeAllViews()

        for (cmd in commands) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val isLongDesc = cmd.description.length > 35
                val rowH = if (isLongDesc) dp(56.4f) else dp(37.5f)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    rowH
                )
                setPadding(dp(16f), 0, dp(16f), 0)
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)

                setOnClickListener {
                    onCommandSelected?.invoke(cmd.command)
                }
            }

            // Description à gauche (16dp bord)
            val descView = TextView(context).apply {
                text = cmd.description
                setTextColor(ThemeColors.BOT_MENU_TEXT_PRIMARY)
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(descView)

            // Commande à droite (/cmd en gris)
            val cmdView = TextView(context).apply {
                text = cmd.command
                setTextColor(ThemeColors.BOT_MENU_TEXT_COMMAND)
                textSize = 15f
                typeface = TypefaceManager.getMedium(context)
            }
            row.addView(cmdView)

            container.addView(row)
        }
    }
}
