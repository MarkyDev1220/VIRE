package com.example.vire.android

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private var messages: MutableList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_ME = 1
    private val TYPE_OTHER = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender == "Me") TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ME) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_me, parent, false)
            MeViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_other, parent, false)
            OtherViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is MeViewHolder) holder.text.text = message.content
        if (holder is OtherViewHolder) holder.text.text = message.content
    }

    override fun getItemCount() = messages.size

    fun updateData(newMessages: List<Message>) {
        messages = newMessages.toMutableList()
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    class MeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.messageTextMe)
    }

    class OtherViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.messageTextOther)
    }
}