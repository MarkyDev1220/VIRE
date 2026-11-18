package com.vire.android.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class UserChatSearchAdapter(
    initialUsers: List<String>,
    private val onUserSelected: (String) -> Unit
) : RecyclerView.Adapter<UserChatSearchAdapter.ViewHolder>() {

    private var allUsers: List<String> = initialUsers.distinct()
    private var visibleUsers: List<String> = allUsers

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.chatUserName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val username = visibleUsers[position]
        holder.name.text = username
        holder.itemView.setOnClickListener { onUserSelected(username) }
    }

    override fun getItemCount(): Int = visibleUsers.size

    fun updateUsers(users: List<String>) {
        allUsers = users.distinct()
        visibleUsers = allUsers
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        visibleUsers = if (query.isBlank()) {
            allUsers
        } else {
            allUsers.filter { it.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }
}
