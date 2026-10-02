package com.vire.android.android

import android.graphics.Color
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

class UserSearchAdapter(
    initialUsers: List<UserSearchItem> = emptyList(),
    private val onUserClick: (UserSearchItem) -> Unit,
    private val onActionClick: (UserSearchItem) -> Unit
) : ListAdapter<UserSearchItem, UserSearchAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_search, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.userSearchAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.userSearchName)
        private val tvArea: TextView = itemView.findViewById(R.id.userSearchArea)
        private val tvSkill: TextView = itemView.findViewById(R.id.userSearchSkill)
        private val tvBio: TextView = itemView.findViewById(R.id.userSearchBio)
        private val btnAction: Button = itemView.findViewById(R.id.userSearchActionButton)

        fun bind(item: UserSearchItem) {
            val user = item.user
            tvName.text = user.username

            // Local area
            if (user.localArea.isNotBlank()) {
                tvArea.text = "📍 ${user.localArea}"
                tvArea.visibility = View.VISIBLE
            } else {
                tvArea.visibility = View.GONE
            }

            // Skill level
            if (user.skillLevel.isNotBlank()) {
                tvSkill.text = user.skillLevel
                tvSkill.visibility = View.VISIBLE
            } else {
                tvSkill.visibility = View.GONE
            }

            // Bio / Fav Games snippet
            val bioText = when {
                user.gamerBio.isNotBlank() -> user.gamerBio
                user.favoriteGames.isNotEmpty() -> "Plays: ${user.favoriteGames.joinToString(", ")}"
                user.favoriteGenres.isNotEmpty() -> "Genres: ${user.favoriteGenres.joinToString(", ")}"
                else -> "VIRE Gamer"
            }
            tvBio.text = bioText

            // Avatar image
            if (!item.profileImageUrl.isNullOrBlank()) {
                Picasso.get()
                    .load(item.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
            }

            // Friendship action button state
            when (item.friendshipState) {
                FriendManager.FriendshipState.NONE -> {
                    btnAction.text = "Add Friend"
                    btnAction.isEnabled = true
                    btnAction.visibility = View.VISIBLE
                    btnAction.setBackgroundColor(Color.parseColor("#1976D2")) // Blue
                }
                FriendManager.FriendshipState.PENDING_SENT -> {
                    btnAction.text = "Pending"
                    btnAction.isEnabled = false
                    btnAction.visibility = View.VISIBLE
                    btnAction.setBackgroundColor(Color.parseColor("#9E9E9E")) // Grey
                }
                FriendManager.FriendshipState.PENDING_RECEIVED -> {
                    btnAction.text = "Accept"
                    btnAction.isEnabled = true
                    btnAction.visibility = View.VISIBLE
                    btnAction.setBackgroundColor(Color.parseColor("#388E3C")) // Green
                }
                FriendManager.FriendshipState.FRIENDS -> {
                    btnAction.text = "Friends ✓"
                    btnAction.isEnabled = false
                    btnAction.visibility = View.VISIBLE
                    btnAction.setBackgroundColor(Color.parseColor("#616161")) // Dark Grey
                }
            }

            itemView.setOnClickListener { onUserClick(item) }
            btnAction.setOnClickListener { onActionClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UserSearchItem>() {
            override fun areItemsTheSame(oldItem: UserSearchItem, newItem: UserSearchItem): Boolean =
                oldItem.user.id == newItem.user.id

            override fun areContentsTheSame(oldItem: UserSearchItem, newItem: UserSearchItem): Boolean =
                oldItem == newItem
        }
    }
}

data class UserSearchItem(
    val user: User,
    val profileImageUrl: String? = null,
    val friendshipState: FriendManager.FriendshipState = FriendManager.FriendshipState.NONE,
    val requestId: String? = null
)
