package com.vire.android.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class InboxConversationAdapter(
    private var allConversations: List<ConversationItem>,
    private val onConversationClick: (ConversationItem) -> Unit,
    private val onConversationMenuClick: (ConversationItem, View) -> Unit
) : RecyclerView.Adapter<InboxConversationAdapter.ViewHolder>() {

    enum class FilterType {
        ALL,
        UNREAD,
        GROUPS,
        ARCHIVED
    }

    private var filteredConversations: List<ConversationItem> = allConversations

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.ivConversationAvatar)
        val name: TextView = view.findViewById(R.id.tvConversationName)
        val lastMessage: TextView = view.findViewById(R.id.tvConversationLastMessage)
        val time: TextView = view.findViewById(R.id.tvConversationTime)
        val unreadBadge: TextView = view.findViewById(R.id.tvUnreadBadge)
        val options: ImageButton = view.findViewById(R.id.btnConversationOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inbox_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredConversations[position]
        holder.name.text = item.name
        holder.lastMessage.text = item.lastMessage
        holder.time.text = item.timeFormatted

        if (item.unreadCount > 0) {
            holder.unreadBadge.visibility = View.VISIBLE
            holder.unreadBadge.text = item.unreadCount.toString()
            holder.name.setTextColor(holder.itemView.context.getColor(R.color.circuit_text_primary))
        } else {
            holder.unreadBadge.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onConversationClick(item)
        }
        holder.options.setOnClickListener { anchor ->
            onConversationMenuClick(item, anchor)
        }
    }

    override fun getItemCount(): Int = filteredConversations.size

    fun updateData(conversations: List<ConversationItem>) {
        allConversations = conversations
        filteredConversations = conversations
        notifyDataSetChanged()
    }

    fun applyFilter(filterType: FilterType, searchQuery: String = "") {
        val query = searchQuery.trim().lowercase()
        filteredConversations = allConversations.filter { item ->
            val matchesFilter = when (filterType) {
                FilterType.ALL -> !item.archived
                FilterType.UNREAD -> !item.archived && item.unreadCount > 0
                FilterType.GROUPS -> !item.archived && item.isGroup
                FilterType.ARCHIVED -> item.archived
            }
            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                item.name.lowercase().contains(query) || item.lastMessage.lowercase().contains(query)
            }
            matchesFilter && matchesSearch
        }
        notifyDataSetChanged()
    }
}
