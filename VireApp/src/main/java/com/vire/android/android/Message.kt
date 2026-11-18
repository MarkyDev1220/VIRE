package com.vire.android.android

data class Message(
    val sender: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = "",
    val senderUid: String = ""
)
