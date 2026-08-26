package com.vire.android.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class ChatAdapter(
    private val messages: MutableList<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_ME = 0
        private const val VIEW_OTHER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender == "Me") VIEW_ME else VIEW_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_ME) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_me, parent, false)
            MeMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_other, parent, false)
            OtherMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is MeMessageViewHolder -> holder.bind(message)
            is OtherMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    fun notifyNewMessage() {
        notifyItemInserted(messages.size - 1)
    }

    class MeMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val msgText: TextView = itemView.findViewById(R.id.messageTextMe)
        fun bind(message: Message) {
            msgText.text = message.content
        }
    }

    class OtherMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val msgText: TextView = itemView.findViewById(R.id.messageTextOther)
        fun bind(message: Message) {
            msgText.text = message.content
        }
    }
}


