package com.vire.android.android

data class Post(
    val id: String = java.util.UUID.randomUUID().toString(),
    val username: String,
    val content: String,
    val imageUri: String? = null, // NEW: optional image
    var likes: Int = 0,
    val comments: MutableList<Comment> = mutableListOf(), // NEW: comment objects
    val timestamp: Long = System.currentTimeMillis()
) : java.io.Serializable

data class Comment(
    val username: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) : java.io.Serializable

