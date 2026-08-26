package com.vire.android.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class MessagesListAdapter(
    private var fullList: List<Pair<String, Message?>>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<MessagesListAdapter.ViewHolder>() {

    private var filteredList: List<Pair<String, Message?>> = fullList

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.userNameText)
        val lastMessage: TextView = view.findViewById(R.id.lastMessageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (user, message) = filteredList[position]
        holder.userName.text = user
        holder.lastMessage.text = message?.content ?: ""
        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = filteredList.size

    fun updateData(newData: List<Pair<String, Message?>>) {
        fullList = newData
        filteredList = newData
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredList = if (query.isBlank()) {
            fullList
        } else {
            fullList.filter { it.first.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }
}
