package com.vire.android.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class DeckCommentAdapter(
    initial: List<DeckComment> = emptyList()
) : ListAdapter<DeckComment, DeckCommentAdapter.VH>(DIFF) {

    init {
        submitList(initial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_deck_comment, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAuthor: TextView = itemView.findViewById(R.id.commentAuthor)
        private val tvMessage: TextView = itemView.findViewById(R.id.commentMessage)

        fun bind(comment: DeckComment) {
            tvAuthor.text = comment.authorName.ifBlank { "Gamer" }
            tvMessage.text = comment.message
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DeckComment>() {
            override fun areItemsTheSame(oldItem: DeckComment, newItem: DeckComment): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DeckComment, newItem: DeckComment): Boolean =
                oldItem == newItem
        }
    }
}
