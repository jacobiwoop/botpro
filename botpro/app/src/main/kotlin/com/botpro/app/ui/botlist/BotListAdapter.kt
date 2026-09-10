package com.botpro.app.ui.botlist

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.botpro.app.R
import com.botpro.app.data.model.Conversation
import com.botpro.app.core.utils.AndroidUtilities

/**
 * Adapter pour la liste des conversations bot.
 * Reproduit fidèlement le rendu de `DialogCell` de Telegram :
 * - Avatar rond coloré avec initiales
 * - Nom du bot (gras) + icône bot
 * - Dernier message (gris)
 * - Heure alignée à droite
 * - Badge de messages non lus (bulle bleue)
 */
class BotListAdapter(
    private val conversations: List<Conversation>,
    private val onItemClick: (Conversation) -> Unit
) : RecyclerView.Adapter<BotListAdapter.ViewHolder>() {

    // Couleurs d'avatar comme dans Telegram (palette de 7 couleurs)
    private val avatarColors = intArrayOf(
        0xFFE17076.toInt(), // Rouge
        0xFFEDA86C.toInt(), // Orange
        0xFF7BC862.toInt(), // Vert
        0xFF6EC9CB.toInt(), // Teal
        0xFF65AADD.toInt(), // Bleu
        0xFFEE7AAE.toInt(), // Rose
        0xFFA695E7.toInt(), // Violet
    )

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarView: View = itemView.findViewById(R.id.avatar_view)
        val avatarText: TextView = itemView.findViewById(R.id.avatar_text)
        val nameText: TextView = itemView.findViewById(R.id.name_text)
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val timeText: TextView = itemView.findViewById(R.id.time_text)
        val unreadBadge: TextView = itemView.findViewById(R.id.unread_badge)
        val onlineIndicator: View = itemView.findViewById(R.id.online_indicator)
        val divider: View = itemView.findViewById(R.id.divider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        val bot = conversation.bot

        // Avatar avec initiales et couleur (comme Telegram)
        val colorIndex = Math.abs(bot.id.hashCode()) % avatarColors.size
        holder.avatarView.setBackgroundColor(avatarColors[colorIndex])
        holder.avatarText.text = bot.name.take(1).uppercase()

        // Nom du bot
        holder.nameText.text = bot.name

        // Dernier message
        holder.messageText.text = conversation.lastMessage?.text ?: ""

        // Heure relative (comme Telegram)
        conversation.lastMessage?.let { msg ->
            holder.timeText.text = DateUtils.getRelativeTimeSpanString(
                msg.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
        }

        // Badge messages non lus
        if (conversation.unreadCount > 0) {
            holder.unreadBadge.visibility = View.VISIBLE
            holder.unreadBadge.text = conversation.unreadCount.toString()
        } else {
            holder.unreadBadge.visibility = View.GONE
        }

        // Indicateur en ligne
        holder.onlineIndicator.visibility = if (bot.isOnline) View.VISIBLE else View.GONE

        // Masquer le divider pour le dernier élément
        holder.divider.visibility = if (position == conversations.size - 1) View.GONE else View.VISIBLE

        // Click listener
        holder.itemView.setOnClickListener { onItemClick(conversation) }
    }

    override fun getItemCount(): Int = conversations.size
}
