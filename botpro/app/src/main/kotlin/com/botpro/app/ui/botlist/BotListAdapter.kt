package com.botpro.app.ui.botlist

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.botpro.app.data.model.Conversation
import com.botpro.app.ui.dialogs.DialogCellView

/**
 * Adapter pour la liste des conversations de bots.
 * Utilise directement la vue dessinée sur Canvas [DialogCellView] sans passer par un layout XML.
 */
class BotListAdapter(
    private var conversations: List<Conversation>,
    private val onItemClick: (Conversation) -> Unit
) : RecyclerView.Adapter<BotListAdapter.ViewHolder>() {

    class ViewHolder(val cellView: DialogCellView) : RecyclerView.ViewHolder(cellView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cellView = DialogCellView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return ViewHolder(cellView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.cellView.setConversation(conversation)
        holder.cellView.setOnClickListener {
            onItemClick(conversation)
        }
    }

    override fun getItemCount(): Int = conversations.size

    fun updateConversations(newConversations: List<Conversation>) {
        conversations = newConversations
        notifyDataSetChanged()
    }
}
