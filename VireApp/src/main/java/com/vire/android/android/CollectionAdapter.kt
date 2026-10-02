package com.vire.android.android

import android.graphics.Color
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

class CollectionAdapter(
    initial: List<CollectionItem> = emptyList(),
    private val onItemAction: (CollectionItem, Action) -> Unit
) : ListAdapter<CollectionItem, CollectionAdapter.VH>(DIFF) {

    enum class Action { VIEW, EDIT, DELETE, CHANGE_STATUS }

    init {
        submitList(initial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_collection, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.collectionItemImage)
        private val tvTitle: TextView = itemView.findViewById(R.id.collectionItemTitle)
        private val tvTypeBadge: TextView = itemView.findViewById(R.id.collectionItemTypeBadge)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.collectionItemStatusBadge)
        private val tvCategory: TextView = itemView.findViewById(R.id.collectionItemCategory)
        private val tvNotes: TextView = itemView.findViewById(R.id.collectionItemNotes)
        private val tvPrice: TextView = itemView.findViewById(R.id.collectionItemPrice)
        private val btnMore: ImageButton = itemView.findViewById(R.id.collectionItemMoreButton)

        fun bind(item: CollectionItem) {
            tvTitle.text = item.title
            tvTypeBadge.text = item.itemType
            tvStatusBadge.text = item.statusTag
            tvCategory.text = item.category

            // Status Badge Color
            val statusColor = when (item.statusTag.uppercase()) {
                "OWNED" -> "#4CAF50"   // Green
                "WANT" -> "#2196F3"    // Blue
                "TRADING" -> "#FF9800" // Orange
                "SELLING" -> "#E91E63" // Pink/Red
                else -> "#757575"      // Grey
            }
            try {
                tvStatusBadge.background?.setTint(Color.parseColor(statusColor))
            } catch (e: Exception) {
                // Ignore tint error if drawable fails
            }

            // Notes
            if (item.notes.isNotBlank()) {
                tvNotes.text = item.notes
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }

            // Price
            if (item.price > 0.0) {
                tvPrice.text = String.format("$%.2f", item.price)
                tvPrice.visibility = View.VISIBLE
            } else {
                tvPrice.visibility = View.GONE
            }

            // Image
            if (item.imageUrl.isNotBlank()) {
                if (item.imageUrl.startsWith("http://") || item.imageUrl.startsWith("https://")) {
                    Picasso.get()
                        .load(item.imageUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(ivImage)
                } else {
                    try {
                        ivImage.setImageURI(Uri.parse(item.imageUrl))
                    } catch (e: Exception) {
                        ivImage.setImageResource(R.drawable.ic_placeholder)
                    }
                }
            } else {
                ivImage.setImageResource(R.drawable.ic_placeholder)
            }

            // Click listeners
            itemView.setOnClickListener { onItemAction(item, Action.VIEW) }

            btnMore.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add("View / Details")
                popup.menu.add("Edit Item")
                popup.menu.add("Delete Item")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "View / Details" -> onItemAction(item, Action.VIEW)
                        "Edit Item" -> onItemAction(item, Action.EDIT)
                        "Delete Item" -> onItemAction(item, Action.DELETE)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CollectionItem>() {
            override fun areItemsTheSame(old: CollectionItem, new: CollectionItem): Boolean = old.id == new.id
            override fun areContentsTheSame(old: CollectionItem, new: CollectionItem): Boolean = old == new
        }
    }
}
