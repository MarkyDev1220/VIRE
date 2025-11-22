package com.example.vire.android

data class NotificationItem(
    val id: Long,
    val toUser: String,
    val title: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    var read: Boolean = false
)