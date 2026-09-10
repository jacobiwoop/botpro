package com.botpro.app.ui.chat

import android.text.format.DateFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpro.app.R
import com.botpro.app.data.model.InlineButton
import com.botpro.app.data.model.Message
import java.util.Date

/**
 * Adapter pour les messages du chat.
 * Reproduit fidèlement le rendu des bulles de `ChatMessageCell` de Telegram :
 * - Messages sortants : bulle bleue (droite)
 * - Messages entrants : bulle sombre (gauche)
 * - Heure en bas à droite de la bulle
 * - Boutons inline sous le message (clavier de bot)
 */
class ChatMessagesAdapter(
    private val messages: List<Message>,
    private val onButtonClick: ((InlineButton) -> Unit)? = null
) : RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bubbleContainer: FrameLayout = itemView.findViewById(R.id.bubble_container)
        val bubbleView: LinearLayout = itemView.findViewById(R.id.bubble_view)
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val timeText: TextView = itemView.findViewById(R.id.time_text)
        val inlineKeyboard: LinearLayout = itemView.findViewById(R.id.inline_keyboard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        val context = holder.itemView.context

        // Texte du message
        holder.messageText.text = message.text

        // Heure (format comme Telegram : HH:mm)
        holder.timeText.text = DateFormat.format("HH:mm", Date(message.timestamp))

        // Direction de la bulle (comme ChatMessageCell)
        val params = holder.bubbleView.layoutParams as FrameLayout.LayoutParams
        if (message.isOutgoing) {
            // Bulle sortante → droite, couleur bleue
            params.gravity = Gravity.END
            params.marginStart = context.resources.getDimensionPixelSize(R.dimen.bubble_margin_large)
            params.marginEnd = context.resources.getDimensionPixelSize(R.dimen.bubble_margin_small)
            holder.bubbleView.setBackgroundColor(context.getColor(R.color.bubble_out))
        } else {
            // Bulle entrante → gauche, couleur sombre
            params.gravity = Gravity.START
            params.marginStart = context.resources.getDimensionPixelSize(R.dimen.bubble_margin_small)
            params.marginEnd = context.resources.getDimensionPixelSize(R.dimen.bubble_margin_large)
            holder.bubbleView.setBackgroundColor(context.getColor(R.color.bubble_in))
        }
        holder.bubbleView.layoutParams = params

        // Clavier inline de bot (comme BotKeyboardView de Telegram)
        holder.inlineKeyboard.removeAllViews()
        message.replyMarkup?.let { markup ->
            holder.inlineKeyboard.visibility = View.VISIBLE
            for (row in markup.rows) {
                val rowLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 4
                    }
                }
                for (button in row) {
                    val btnView = createInlineButton(context, button)
                    rowLayout.addView(btnView)
                }
                holder.inlineKeyboard.addView(rowLayout)
            }
        } ?: run {
            holder.inlineKeyboard.visibility = View.GONE
        }
    }

    private fun createInlineButton(context: android.content.Context, button: InlineButton): TextView {
        return TextView(context).apply {
            text = button.text
            setTextColor(context.getColor(R.color.bot_keyboard_button_text))
            setBackgroundColor(context.getColor(R.color.bot_keyboard_button))
            gravity = Gravity.CENTER
            setPadding(24, 20, 24, 20)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = 4
                marginEnd = 4
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onButtonClick?.invoke(button)
            }
        }
    }

    override fun getItemCount(): Int = messages.size
}
