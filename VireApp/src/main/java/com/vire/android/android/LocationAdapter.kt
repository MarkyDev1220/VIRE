package com.vire.android.android

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vire.android.R

class LocationAdapter(
    initial: List<GamingLocation> = emptyList(),
    private val onLocationClick: (GamingLocation) -> Unit
) : ListAdapter<GamingLocation, LocationAdapter.VH>(DIFF) {

    init {
        submitList(initial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_location, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.locationImage)
        private val tvName: TextView = itemView.findViewById(R.id.locationName)
        private val tvTypeBadge: TextView = itemView.findViewById(R.id.locationTypeBadge)
        private val tvClaimedBadge: TextView = itemView.findViewById(R.id.locationClaimedBadge)
        private val tvRating: TextView = itemView.findViewById(R.id.locationRating)
        private val tvAddress: TextView = itemView.findViewById(R.id.locationAddress)
        private val tvGames: TextView = itemView.findViewById(R.id.locationGames)

        fun bind(location: GamingLocation) {
            tvName.text = location.name
            tvTypeBadge.text = location.type

            tvClaimedBadge.visibility = if (location.isClaimed) View.VISIBLE else View.GONE

            val avg = location.averageRating()
            tvRating.text = "★ ${"%.1f".format(avg)} (${location.reviewCount} reviews)"

            val fullAddr = listOf(location.address, location.city, location.state)
                .filter { it.isNotBlank() }
                .joinToString(", ")
            tvAddress.text = fullAddr.ifBlank { "Location address not specified" }

            if (location.supportedGames.isNotEmpty()) {
                tvGames.text = "Games: ${location.supportedGames.joinToString(", ")}"
                tvGames.visibility = View.VISIBLE
            } else {
                tvGames.visibility = View.GONE
            }

            if (location.imageUrl.isNotBlank()) {
                if (location.imageUrl.startsWith("http://") || location.imageUrl.startsWith("https://")) {
                    Picasso.get()
                        .load(location.imageUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(ivImage)
                } else {
                    try {
                        ivImage.setImageURI(Uri.parse(location.imageUrl))
                    } catch (e: Exception) {
                        ivImage.setImageResource(R.drawable.ic_placeholder)
                    }
                }
            } else {
                ivImage.setImageResource(R.drawable.ic_placeholder)
            }

            itemView.setOnClickListener { onLocationClick(location) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GamingLocation>() {
            override fun areItemsTheSame(oldItem: GamingLocation, newItem: GamingLocation): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: GamingLocation, newItem: GamingLocation): Boolean =
                oldItem == newItem
        }
    }
}
