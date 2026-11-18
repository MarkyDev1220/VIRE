package com.vire.android.android

data class ConversationItem(
    val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val timeFormatted: String = "",
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val otherUid: String = "",
    val archived: Boolean = false
)
