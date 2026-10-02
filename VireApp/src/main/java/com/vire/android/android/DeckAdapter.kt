package com.vire.android.android

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vire.android.R

class DeckAdapter(
    initial: List<Deck> = emptyList(),
    private val onDeckClick: (Deck) -> Unit,
    private val onOptionClick: (Deck, Action) -> Unit
) : ListAdapter<Deck, DeckAdapter.VH>(DIFF) {

    enum class Action { EDIT, DELETE, SHARE }

    init {
        submitList(initial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_deck, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.deckImage)
        private val tvTitle: TextView = itemView.findViewById(R.id.deckTitle)
        private val tvGameBadge: TextView = itemView.findViewById(R.id.deckGameBadge)
        private val tvFormatBadge: TextView = itemView.findViewById(R.id.deckFormatBadge)
        private val tvCardCount: TextView = itemView.findViewById(R.id.deckCardCount)
        private val tvLikesCount: TextView = itemView.findViewById(R.id.deckLikesCount)
        private val btnMore: ImageButton = itemView.findViewById(R.id.deckMoreButton)

        fun bind(deck: Deck) {
            tvTitle.text = deck.deckName
            tvGameBadge.text = deck.gameSystem
            tvFormatBadge.text = deck.format.ifBlank { "Standard" }
            tvCardCount.text = "🃏 ${deck.cardCount} Cards"
            tvLikesCount.text = "❤️ ${deck.likesCount}"

            if (deck.coverImageUrl.isNotBlank()) {
                if (deck.coverImageUrl.startsWith("http://") || deck.coverImageUrl.startsWith("https://")) {
                    Picasso.get()
                        .load(deck.coverImageUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(ivImage)
                } else {
                    try {
                        ivImage.setImageURI(Uri.parse(deck.coverImageUrl))
                    } catch (e: Exception) {
                        ivImage.setImageResource(R.drawable.ic_placeholder)
                    }
                }
            } else {
                ivImage.setImageResource(R.drawable.ic_placeholder)
            }

            itemView.setOnClickListener { onDeckClick(deck) }

            btnMore.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add("Edit Deck")
                popup.menu.add("Delete Deck")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Edit Deck" -> onOptionClick(deck, Action.EDIT)
                        "Delete Deck" -> onOptionClick(deck, Action.DELETE)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Deck>() {
            override fun areItemsTheSame(oldItem: Deck, newItem: Deck): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Deck, newItem: Deck): Boolean =
                oldItem == newItem
        }
    }
}
