package com.vire.android.android

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vire.android.R

class CommunityPostAdapter(
    initial: List<CommunityPost> = emptyList(),
    private val currentUid: String = "",
    private val onLikeClick: (CommunityPost) -> Unit
) : ListAdapter<CommunityPost, CommunityPostAdapter.VH>(DIFF) {

    init {
        submitList(initial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_community_post, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.postAuthorAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.postAuthorName)
        private val tvTypeBadge: TextView = itemView.findViewById(R.id.postTypeBadge)
        private val tvContent: TextView = itemView.findViewById(R.id.postContent)
        private val ivPostImage: ImageView = itemView.findViewById(R.id.postImage)
        private val btnLike: Button = itemView.findViewById(R.id.btnLikePost)

        fun bind(p: CommunityPost) {
            tvName.text = p.authorName
            tvTypeBadge.text = p.postType
            tvContent.text = p.content

            val isLiked = p.likedBy.contains(currentUid)
            btnLike.text = if (isLiked) "❤️ Liked (${p.likesCount})" else "🤍 Like (${p.likesCount})"

            if (p.authorAvatarUrl.isNotBlank()) {
                Picasso.get()
                    .load(p.authorAvatarUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
            }

            if (p.imageUrl.isNotBlank()) {
                ivPostImage.visibility = View.VISIBLE
                if (p.imageUrl.startsWith("http://") || p.imageUrl.startsWith("https://")) {
                    Picasso.get().load(p.imageUrl).into(ivPostImage)
                } else {
                    try {
                        ivPostImage.setImageURI(Uri.parse(p.imageUrl))
                    } catch (e: Exception) {
                        ivPostImage.setImageResource(R.drawable.ic_placeholder)
                    }
                }
            } else {
                ivPostImage.visibility = View.GONE
            }

            btnLike.setOnClickListener { onLikeClick(p) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CommunityPost>() {
            override fun areItemsTheSame(oldItem: CommunityPost, newItem: CommunityPost): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CommunityPost, newItem: CommunityPost): Boolean =
                oldItem == newItem
        }
    }
}
