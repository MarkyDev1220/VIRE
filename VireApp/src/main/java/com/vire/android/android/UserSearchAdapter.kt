package com.vire.android.android

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.cardview.widget.CardView
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
        private val card: CardView = itemView.findViewById(R.id.userSearchCard)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.userSearchAvatar)
        private val ivCover: ImageView = itemView.findViewById(R.id.userSearchCover)
        private val tvName: TextView = itemView.findViewById(R.id.userSearchName)
        private val tvHandle: TextView = itemView.findViewById(R.id.userSearchHandle)
        private val tvArea: TextView = itemView.findViewById(R.id.userSearchArea)
        private val tvSkill: TextView = itemView.findViewById(R.id.userSearchSkill)
        private val tvBio: TextView = itemView.findViewById(R.id.userSearchBio)
        private val btnAction: Button = itemView.findViewById(R.id.userSearchActionButton)

        fun bind(item: UserSearchItem) {
            val user = item.user
            val isCurrent = (user.id == com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)
            val themeColors = when (item.profileTheme?.lowercase()) {
                "light" -> Triple("#F4F5F7", "#20242B", "#626B78")
                "blue" -> Triple("#16324F", "#F4F7FB", "#B8C8D8")
                "green" -> Triple("#123C32", "#F1FAF6", "#B5D2C8")
                "purple" -> Triple("#2D1B45", "#FAF4FF", "#D1C1E2")
                else -> Triple("#1B1F24", "#F4F6F8", "#AAB2BC")
            }
            card.setCardBackgroundColor(Color.parseColor(themeColors.first))
            tvName.setTextColor(Color.parseColor(themeColors.second))
            tvHandle.setTextColor(Color.parseColor(themeColors.third))
            tvArea.setTextColor(Color.parseColor(themeColors.third))
            tvSkill.setTextColor(Color.parseColor(themeColors.second))
            tvBio.setTextColor(Color.parseColor(themeColors.third))

            val displayName = user.fullName.ifBlank { user.username }
            tvName.text = if (isCurrent) "$displayName (You)" else displayName
            tvHandle.text = "@${user.username}"

            // Local area
            val profileDetails = user.localArea
            if (profileDetails.isNotBlank()) {
                tvArea.text = "📍 $profileDetails"
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
                user.aboutMe.isNotBlank() -> user.aboutMe
                user.favoriteGames.isNotEmpty() -> "Plays: ${user.favoriteGames.joinToString(", ")}"
                user.favoriteGenres.isNotEmpty() -> "Genres: ${user.favoriteGenres.joinToString(", ")}"
                else -> "VIRE Gamer"
            }
            tvBio.text = bioText

            val defaultAvatar = R.drawable.ic_profile_placeholder
            val coverUrl = item.coverImageUrl
            if (!coverUrl.isNullOrBlank()) {
                loadProfileImage(coverUrl, ivCover, R.drawable.default_cover)
                ivCover.visibility = View.VISIBLE
            } else {
                ivCover.visibility = View.GONE
            }

            val url = item.profileImageUrl
            if (!url.isNullOrBlank() && !url.startsWith("content://")) {
                loadProfileImage(url, ivAvatar, defaultAvatar)
            } else {
                ivAvatar.setImageResource(defaultAvatar)
            }

            // Friendship action button state
            if (isCurrent) {
                btnAction.visibility = View.GONE
            } else {
                when (item.friendshipState) {
                    FriendManager.FriendshipState.NONE -> {
                        btnAction.text = "Add Friend"
                        btnAction.isEnabled = true
                        btnAction.visibility = View.VISIBLE
                        btnAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(itemView.context, R.color.circuit_lime)
                            )
                        )
                        btnAction.setTextColor(
                            androidx.core.content.ContextCompat.getColor(itemView.context, R.color.circuit_black)
                        )
                    }
                    FriendManager.FriendshipState.PENDING_SENT -> {
                        btnAction.text = "Pending"
                        btnAction.isEnabled = false
                        btnAction.visibility = View.VISIBLE
                        btnAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(itemView.context, R.color.circuit_lime)
                            )
                        )
                        btnAction.setTextColor(
                            androidx.core.content.ContextCompat.getColor(itemView.context, R.color.circuit_black)
                        )
                    }
                    FriendManager.FriendshipState.PENDING_RECEIVED -> {
                        btnAction.text = "Accept"
                        btnAction.isEnabled = true
                        btnAction.visibility = View.VISIBLE
                        btnAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(itemView.context, R.color.circuit_lime)
                            )
                        )
                        btnAction.setTextColor(
                            androidx.core.content.ContextCompat.getColor(itemView.context, R.color.circuit_black)
                        )
                    }
                    FriendManager.FriendshipState.FRIENDS -> {
                        btnAction.text = "Friends ✓"
                        btnAction.isEnabled = false
                        btnAction.visibility = View.VISIBLE
                        btnAction.setBackgroundColor(Color.parseColor("#616161")) // Dark Grey
                    }
                }
            }

            itemView.setOnClickListener { onUserClick(item) }
            btnAction.setOnClickListener { onActionClick(item) }
        }

        private fun loadProfileImage(url: String, target: ImageView, placeholder: Int) {
            target.tag = url
            if (url.startsWith("gs://")) {
                com.google.firebase.storage.FirebaseStorage.getInstance()
                    .getReferenceFromUrl(url)
                    .downloadUrl
                    .addOnSuccessListener { downloadUrl ->
                        if (target.tag == url) {
                            Picasso.get().load(downloadUrl).placeholder(placeholder).error(placeholder).into(target)
                        }
                    }
                    .addOnFailureListener {
                        if (target.tag == url) target.setImageResource(placeholder)
                    }
            } else if (url.startsWith("data:image") || url.contains("base64")) {
                try {
                    val pureBase64 = url.substringAfter("base64,")
                    val decodedBytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) target.setImageBitmap(bitmap) else target.setImageResource(placeholder)
                } catch (_: Exception) {
                    target.setImageResource(placeholder)
                }
            } else if (url.startsWith("http")) {
                Picasso.get().load(url).placeholder(placeholder).error(placeholder).into(target)
            } else {
                target.setImageResource(placeholder)
            }
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
    val coverImageUrl: String? = null,
    val profileTheme: String? = null,
    val friendshipState: FriendManager.FriendshipState = FriendManager.FriendshipState.NONE,
    val requestId: String? = null
)
