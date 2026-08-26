package com.example.vire.android

object MessageManager {

    // Map each user to their own list of messages
    private val messagesMap = mutableMapOf<String, MutableList<Message>>()

    // List of ALL users in the system (for new message search)
    private val allUsers = mutableSetOf<String>()

    // Register a user (call this when a user signs up or logs in)
    fun registerUser(username: String) {
        allUsers.add(username)
        messagesMap.getOrPut(username) { mutableListOf() }
    }

    // Get all messages for a specific user
    fun getMessagesForUser(user: String): MutableList<Message> =
        messagesMap.getOrPut(user) { mutableListOf() }

    // Send a message to a specific user's chat
    fun sendMessageForUser(user: String, message: Message) {
        messagesMap.getOrPut(user) { mutableListOf() }.add(message)
        allUsers.add(user) // ensure user appears in new message search
    }

    // Simulate receiving a message from a specific user
    fun receiveMessageForUser(user: String, message: Message) {
        messagesMap.getOrPut(user) { mutableListOf() }.add(message)
        allUsers.add(user)
    }

    // Optional: get all messages (flattened)
    fun getAllMessages(): List<Message> = messagesMap.values.flatten()

    // Get last message for each user (for conversation list)
    fun getLastMessagesForAllUsers(): List<Pair<String, Message?>> {
        return messagesMap.map { entry ->
            val lastMessage = entry.value.lastOrNull()
            entry.key to lastMessage
        }.toList()
    }

    // ⭐ NEW: Return all users as message previews (for "Start New Message")
    fun getAllUsersAsMessageList(): List<Pair<String, Message?>> {
        return allUsers.map { username ->
            username to messagesMap[username]?.lastOrNull()
        }
    }
}

