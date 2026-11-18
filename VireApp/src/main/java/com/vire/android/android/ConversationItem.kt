package com.vire.android.android

data class ConversationItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timeFormatted: String,
    val unreadCount: Int,
    val isGroup: Boolean,
    val isArchived: Boolean = false,
    val isHidden: Boolean = false
)
