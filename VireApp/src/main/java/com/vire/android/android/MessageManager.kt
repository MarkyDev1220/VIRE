package com.vire.android.android

object MessageManager {

    // Map each user/group to their own list of messages
    private val messagesMap = mutableMapOf<String, MutableList<Message>>()

    // Map user/group to unread message count
    private val unreadMap = mutableMapOf<String, Int>()

    // Set of group conversation names
    private val groupSet = mutableSetOf<String>()

    // Custom time labels for conversations
    private val timeLabelMap = mutableMapOf<String, String>()

    // List of ALL real users in the system (for new message search)
    private val allUsers = mutableSetOf<String>()

    // Clear all conversations or reset
    fun clearAll() {
        messagesMap.clear()
        unreadMap.clear()
        groupSet.clear()
        timeLabelMap.clear()
        allUsers.clear()
    }

    // Register real app users
    fun setRegisteredUsers(usernames: List<String>) {
        allUsers.clear()
        allUsers.addAll(usernames)
    }

    // Register a user
    fun registerUser(username: String) {
        if (username.isNotBlank()) {
            allUsers.add(username)
            messagesMap.getOrPut(username) { mutableListOf() }
        }
    }

    // Get all messages for a specific user
    fun getMessagesForUser(user: String): MutableList<Message> =
        messagesMap.getOrPut(user) { mutableListOf() }

    // Send a message to a specific user's chat
    fun sendMessageForUser(user: String, message: Message) {
        messagesMap.getOrPut(user) { mutableListOf() }.add(message)
        allUsers.add(user)
        unreadMap[user] = 0
        timeLabelMap[user] = "Just now"
    }

    // Simulate receiving a message from a specific user
    fun receiveMessageForUser(user: String, message: Message) {
        messagesMap.getOrPut(user) { mutableListOf() }.add(message)
        allUsers.add(user)
        unreadMap[user] = (unreadMap[user] ?: 0) + 1
        timeLabelMap[user] = "Just now"
    }

    // Mark a specific conversation as read
    fun markAsRead(user: String) {
        unreadMap[user] = 0
    }

    // Mark all conversations as read
    fun markAllAsRead() {
        for (key in messagesMap.keys) {
            unreadMap[key] = 0
        }
    }

    // Total unread messages across all conversations
    fun getTotalUnreadCount(): Int {
        return unreadMap.values.sum()
    }

    // Unread count for a specific user
    fun getUnreadCount(user: String): Int {
        return unreadMap[user] ?: 0
    }

    // Rich conversations list for Inbox (only shows active chats with messages)
    fun getConversationsList(): List<ConversationItem> {
        return messagesMap
            .filter { entry -> entry.value.isNotEmpty() }
            .map { (user, messages) ->
                val lastMsg = messages.lastOrNull()
                val lastContent = lastMsg?.content ?: "Start a conversation"
                val time = timeLabelMap[user] ?: if (lastMsg != null) {
                    formatTimestamp(lastMsg.timestamp)
                } else {
                    "Recent"
                }
                val unread = unreadMap[user] ?: 0
                val isGroup = groupSet.contains(user)

                ConversationItem(
                    id = user,
                    name = user,
                    lastMessage = lastContent,
                    timeFormatted = time,
                    unreadCount = unread,
                    isGroup = isGroup
                )
            }.sortedWith(
                compareByDescending<ConversationItem> { it.unreadCount > 0 }
                    .thenBy { it.name }
            )
    }

    private fun formatTimestamp(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d"
            else -> "Recent"
        }
    }

    fun getAllMessages(): List<Message> = messagesMap.values.flatten()

    fun getLastMessagesForAllUsers(): List<Pair<String, Message?>> {
        return messagesMap.map { entry ->
            val lastMessage = entry.value.lastOrNull()
            entry.key to lastMessage
        }.toList()
    }

    fun getAllUsersAsMessageList(): List<Pair<String, Message?>> {
        return allUsers.map { username ->
            username to messagesMap[username]?.lastOrNull()
        }
    }

    fun getAllUsernames(): List<String> = allUsers.toList()
}
