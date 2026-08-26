package com.vire.android.android

object NotificationStore {
    private val notifications = mutableListOf<NotificationItem>()
    private var lastId = 3000L

    fun nextId(): Long = ++lastId

    fun addNotification(item: NotificationItem) {
        notifications.add(0, item) // newest first
    }

    fun getNotificationsForUser(username: String): List<NotificationItem> {
        return notifications.filter { it.toUser.equals(username, ignoreCase = true) }
    }

    fun markAsRead(id: Long) {
        notifications.find { it.id == id }?.read = true
    }
}