package com.example.vire.android

data class Post(
    val username: String,
    val content: String,
    var likes: Int = 0,
    val comments: MutableList<String> = mutableListOf(),
    val timestamp: Long = System.currentTimeMillis()
) : java.io.Serializable
