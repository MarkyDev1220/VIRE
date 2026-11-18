package com.vire.android.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class InboxConversationAdapter(
    private var conversations: List<ConversationItem>,
    private val onClick: (ConversationItem) -> Unit
) : RecyclerView.Adapter<InboxConversationAdapter.ViewHolder>() {

    enum class FilterType { ALL, UNREAD, GROUPS, ARCHIVED, HIDDEN }

    private var visibleConversations = conversations

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.conversationName)
        val preview: TextView = view.findViewById(R.id.conversationPreview)
        val time: TextView = view.findViewById(R.id.conversationTime)
        val unread: TextView = view.findViewById(R.id.conversationUnreadCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inbox_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = visibleConversations[position]
        holder.name.text = conversation.name
        holder.preview.text = conversation.lastMessage
        holder.time.text = conversation.timeFormatted
        holder.unread.visibility = if (conversation.unreadCount > 0) View.VISIBLE else View.GONE
        holder.unread.text = conversation.unreadCount.toString()
        holder.itemView.setOnClickListener { onClick(conversation) }
    }

    override fun getItemCount(): Int = visibleConversations.size

    fun updateData(newData: List<ConversationItem>) {
        conversations = newData
        visibleConversations = newData
        notifyDataSetChanged()
    }

    fun applyFilter(filterType: FilterType, query: String) {
        visibleConversations = conversations.filter { conversation ->
            val matchesFilter = when (filterType) {
                FilterType.ALL -> !conversation.isArchived && !conversation.isHidden
                FilterType.UNREAD -> conversation.unreadCount > 0 && !conversation.isArchived && !conversation.isHidden
                FilterType.GROUPS -> conversation.isGroup && !conversation.isArchived && !conversation.isHidden
                FilterType.ARCHIVED -> conversation.isArchived
                FilterType.HIDDEN -> conversation.isHidden
            }
            matchesFilter && (query.isBlank() || conversation.name.contains(query, ignoreCase = true))
        }
        notifyDataSetChanged()
    }
}
