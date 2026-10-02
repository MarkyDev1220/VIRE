package com.vire.android.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class StoreReviewAdapter(
    initial: List<StoreReview> = emptyList()
) : ListAdapter<StoreReview, StoreReviewAdapter.VH>(DIFF) {

    init {
        submitList(initial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_store_review, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAuthor: TextView = itemView.findViewById(R.id.reviewAuthor)
        private val tvRating: TextView = itemView.findViewById(R.id.reviewRating)
        private val tvComment: TextView = itemView.findViewById(R.id.reviewComment)

        fun bind(review: StoreReview) {
            tvAuthor.text = review.authorUsername.ifBlank { "Anonymous Gamer" }
            tvRating.text = "★ ${"%.1f".format(review.rating)}"
            tvComment.text = review.comment
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StoreReview>() {
            override fun areItemsTheSame(oldItem: StoreReview, newItem: StoreReview): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: StoreReview, newItem: StoreReview): Boolean =
                oldItem == newItem
        }
    }
}
