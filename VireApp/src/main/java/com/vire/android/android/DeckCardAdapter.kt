package com.vire.android.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class DeckCardAdapter(
    initial: List<DeckCard> = emptyList(),
    private val isEditable: Boolean = true,
    private val onQuantityChange: (DeckCard, Int) -> Unit
) : ListAdapter<DeckCard, DeckCardAdapter.VH>(DIFF) {

    init {
        submitList(initial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_deck_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvQty: TextView = itemView.findViewById(R.id.cardQuantity)
        private val tvName: TextView = itemView.findViewById(R.id.cardName)
        private val tvDetails: TextView = itemView.findViewById(R.id.cardDetails)
        private val btnMinus: ImageButton = itemView.findViewById(R.id.btnMinusQuantity)
        private val btnPlus: ImageButton = itemView.findViewById(R.id.btnPlusQuantity)
        private val quantityControls: View = itemView.findViewById(R.id.quantityControls)

        fun bind(card: DeckCard) {
            tvQty.text = "${card.quantity}x"
            tvName.text = card.cardName
            tvDetails.text = "${card.cardType} • ${card.manaCostOrLevel.ifBlank { "N/A" }}"

            if (isEditable) {
                quantityControls.visibility = View.VISIBLE
                btnMinus.setOnClickListener { onQuantityChange(card, card.quantity - 1) }
                btnPlus.setOnClickListener { onQuantityChange(card, card.quantity + 1) }
            } else {
                quantityControls.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DeckCard>() {
            override fun areItemsTheSame(oldItem: DeckCard, newItem: DeckCard): Boolean =
                oldItem.cardName == newItem.cardName

            override fun areContentsTheSame(oldItem: DeckCard, newItem: DeckCard): Boolean =
                oldItem == newItem
        }
    }
}
