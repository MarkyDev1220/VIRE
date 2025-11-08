package com.example.vire.android

data class Post(
    val username: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) : java.io.Serializable
