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
 * Utilise directement MessageCellView (vue unitaire pure Canvas, 0 vue enfant),
 * reproduisant fidèlement ChatMessageCell de Telegram.
 */
class ChatMessagesAdapter(
    private val messages: List<Message>,
    private val onButtonClick: ((InlineButton) -> Unit)? = null
) : RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder>() {

    class ViewHolder(val cellView: MessageCellView) : RecyclerView.ViewHolder(cellView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cellView = MessageCellView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            onInlineButtonClicked = { btn ->
                onButtonClick?.invoke(btn)
            }
        }
        return ViewHolder(cellView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.cellView.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size
}

