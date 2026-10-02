package com.vire.android.android

import java.io.Serializable

data class CommunityPost(
    val id: String = "",
    val communityId: String = "",
    val authorUid: String = "",
    val authorName: String = "Gamer",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val postType: String = "Discussion", // "Discussion", "Deck Post", "Collection Showcase", "LFG"
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
