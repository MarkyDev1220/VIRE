package com.vire.android.android

data class NotificationItem(
    val id: String = System.currentTimeMillis().toString(),
    val toUid: String = "",
    val toUser: String = "",
    val fromUid: String = "",
    val fromUsername: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL", // e.g., "FRIEND_REQUEST", "COMMENT", "CHALLENGE"
    val relatedId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var read: Boolean = false
) {
    constructor(
        id: Long,
        toUser: String,
        title: String,
        message: String,
        timestamp: Long = System.currentTimeMillis(),
        read: Boolean = false
    ) : this(
        id = id.toString(),
        toUid = "",
        toUser = toUser,
        fromUid = "",
        fromUsername = "",
        title = title,
        message = message,
        type = "CHALLENGE",
        relatedId = "",
        timestamp = timestamp,
        read = read
    )
}
