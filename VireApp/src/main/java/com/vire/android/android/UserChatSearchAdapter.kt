package com.vire.android.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class UserChatSearchAdapter(
    private var allUsers: List<String>,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<UserChatSearchAdapter.ViewHolder>() {

    private var filteredUsers: List<String> = allUsers

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.ivUserAvatar)
        val name: TextView = view.findViewById(R.id.tvUserName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_chat_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = filteredUsers[position]
        holder.name.text = user
        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount(): Int = filteredUsers.size

    fun updateUsers(users: List<String>) {
        allUsers = users
        filteredUsers = users
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val trimmed = query.trim().lowercase()
        filteredUsers = if (trimmed.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { it.lowercase().contains(trimmed) }
        }
        notifyDataSetChanged()
    }
}
