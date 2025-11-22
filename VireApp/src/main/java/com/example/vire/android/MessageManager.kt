package com.example.vire.android

object MessageManager {


    // Map each user to their own list of messages
    private val messagesMap = mutableMapOf<String, MutableList<Message>>()

    // Get all messages for a specific user
    fun getMessagesForUser(user: String): MutableList<Message> =
        messagesMap.getOrPut(user) { mutableListOf() }

    // Send a message to a specific user's chat
    fun sendMessageForUser(user: String, message: Message) {
        messagesMap.getOrPut(user) { mutableListOf() }.add(message)
    }

    // Simulate receiving a message from a specific user
    fun receiveMessageForUser(user: String, message: Message) {
        messagesMap.getOrPut(user) { mutableListOf() }.add(message)
    }

    // Optional: get all messages (flattened, for debugging or global view)
    fun getAllMessages(): List<Message> = messagesMap.values.flatten()

    // Get last message for each user to show in messages list
    fun getLastMessagesForAllUsers(): List<Pair<String, Message?>> {
        return messagesMap.map { entry ->
            val lastMessage = entry.value.lastOrNull()
            entry.key to lastMessage
        }.toList()
    }


}
