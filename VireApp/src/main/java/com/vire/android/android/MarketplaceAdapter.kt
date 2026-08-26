package com.vire.android.android

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R
class MarketplaceAdapter(
    initial: List<MarketplaceItem>,
    private val onItemAction: (MarketplaceItem, Action) -> Unit
) : ListAdapter<MarketplaceItem, MarketplaceAdapter.VH>(DIFF) {

    enum class Action { VIEW, BUY, DELETE }

    init { submitList(initial) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_marketplace, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iv = itemView.findViewById<ImageView>(R.id.itemImage)
        private val tvTitle = itemView.findViewById<TextView>(R.id.itemTitle)
        private val tvPrice = itemView.findViewById<TextView>(R.id.itemPrice)
        private val tvCategory = itemView.findViewById<TextView>(R.id.itemCategory)
        private val btnBuy = itemView.findViewById<Button>(R.id.itemBuyButton)
        private val btnMore = itemView.findViewById<ImageButton>(R.id.itemMoreButton)

        fun bind(item: MarketplaceItem) {
            tvTitle.text = item.title
            tvPrice.text = "$${"%.2f".format(item.price)}"
            tvCategory.text = item.category
            if (item.imageUri != null) iv.setImageURI(Uri.parse(item.imageUri)) else iv.setImageResource(R.drawable.ic_placeholder)

            // click handlers
            itemView.setOnClickListener { onItemAction(item, Action.VIEW) }
            btnBuy.setOnClickListener { onItemAction(item, Action.BUY) }
            btnMore.setOnClickListener {
                // Simple options menu - currently only Delete (could be extended to Edit/Report)
                onItemAction(item, Action.DELETE)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MarketplaceItem>() {
            override fun areItemsTheSame(old: MarketplaceItem, new: MarketplaceItem) = old.id == new.id
            override fun areContentsTheSame(old: MarketplaceItem, new: MarketplaceItem) = old == new
        }
    }
}