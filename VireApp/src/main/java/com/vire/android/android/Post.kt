package com.vire.android.android

import java.io.Serializable

data class Post(
    val id: String = "",
    val authorUid: String = "",
    val username: String = "Gamer",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val imageUri: String? = null,
    val postType: String = "General", // "General", "Deck Post", "Collection Post", "Game Night Post", "Looking for Players"
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

data class Comment(
    val id: String = "",
    val postId: String = "",
    val username: String = "Gamer",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
