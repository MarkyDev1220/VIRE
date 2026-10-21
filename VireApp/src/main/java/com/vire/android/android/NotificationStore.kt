package com.vire.android.android

object NotificationStore {
    private val notifications = mutableListOf<NotificationItem>()
    private var lastId = 3000L

    fun nextId(): Long = ++lastId

    fun addNotification(item: NotificationItem) {
        val existingIndex = notifications.indexOfFirst { it.id == item.id }
        if (existingIndex >= 0) {
            notifications[existingIndex] = item
        } else {
            notifications.add(0, item)
        }
    }

    fun setNotifications(list: List<NotificationItem>) {
        notifications.clear()
        notifications.addAll(list.sortedByDescending { it.timestamp })
    }

    fun getNotificationsForUser(usernameOrUid: String): List<NotificationItem> {
        return notifications.filter {
            it.toUid == usernameOrUid || it.toUser.equals(usernameOrUid, ignoreCase = true)
        }
    }

    fun getAllNotifications(): List<NotificationItem> {
        return notifications.toList()
    }

    fun markAsRead(id: String) {
        notifications.find { it.id == id }?.read = true
    }

    fun markAsRead(id: Long) {
        notifications.find { it.id == id.toString() }?.read = true
    }
}
