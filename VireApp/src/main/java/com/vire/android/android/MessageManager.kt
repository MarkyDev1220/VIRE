package com.vire.android.android

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp

object MessageManager {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val uidByUsername = mutableMapOf<String, String>()
    private val displayNames = mutableListOf<String>()
    private var activeMessagesListener: ListenerRegistration? = null
    private var activeListenerGeneration = 0
    private var activeHiddenMessageIds: Set<String> = emptySet()

    fun setRegisteredUsers(users: Map<String, String>) {
        uidByUsername.clear()
        displayNames.clear()
        users.forEach { (username, uid) ->
            if (username.isNotBlank() && uid.isNotBlank()) {
                uidByUsername[username.lowercase()] = uid
                displayNames.add(username)
            }
        }
    }

    fun registerUser(username: String, uid: String) {
        if (username.isNotBlank() && uid.isNotBlank()) {
            uidByUsername[username.lowercase()] = uid
            if (displayNames.none { it.equals(username, ignoreCase = true) }) displayNames.add(username)
        }
    }

    fun getUserId(username: String): String? = uidByUsername[username.lowercase()]

    fun getAllUsernames(): List<String> = displayNames.toList()

    fun conversationId(firstUid: String, secondUid: String): String =
        listOf(firstUid, secondUid).sorted().joinToString("_")

    fun listenToConversation(
        conversationId: String,
        currentUid: String,
        onMessages: (List<Message>) -> Unit
    ) {
        activeMessagesListener?.remove()
        val generation = ++activeListenerGeneration
        activeHiddenMessageIds = emptySet()
        val settingsRef = db.collection("users").document(currentUid)
            .collection("messageSettings").document(conversationId)
        settingsRef.get().addOnSuccessListener { settings ->
            if (generation != activeListenerGeneration) return@addOnSuccessListener
            activeHiddenMessageIds = (settings.get("deletedMessageIds") as? List<*>)
                ?.filterIsInstance<String>()?.toSet().orEmpty()
            activeMessagesListener = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val messages = snapshot.documents.mapNotNull { document ->
                        if (document.id in activeHiddenMessageIds) return@mapNotNull null
                        val senderUid = document.getString("senderUid") ?: return@mapNotNull null
                        val content = document.getString("content") ?: return@mapNotNull null
                        Message(
                            id = document.id,
                            sender = if (senderUid == currentUid) "Me" else (document.getString("senderName") ?: "Gamer"),
                            content = content,
                            timestamp = document.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis(),
                            senderUid = senderUid
                        )
                    }
                    onMessages(messages)
                }
        }
    }

    fun stopListening() {
        activeListenerGeneration++
        activeMessagesListener?.remove()
        activeMessagesListener = null
    }

    fun sendMessage(
        currentUid: String,
        currentUsername: String,
        otherUid: String,
        otherUsername: String,
        content: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val conversationId = conversationId(currentUid, otherUid)
        val conversationRef = db.collection("conversations").document(conversationId)
        val messageRef = conversationRef.collection("messages").document()
        val participants = listOf(currentUid, otherUid)
        val names = mapOf(currentUid to currentUsername, otherUid to otherUsername)
        val batch = db.batch()

        batch.set(
            conversationRef,
            mapOf(
                "participantUids" to participants,
                "participantNames" to names,
                "lastMessage" to content,
                "lastSenderUid" to currentUid,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
        batch.set(
            messageRef,
            mapOf(
                "senderUid" to currentUid,
                "senderName" to currentUsername,
                "content" to content,
                "timestamp" to FieldValue.serverTimestamp()
            )
        )
        batch.commit()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { error ->
                onResult(false, error.localizedMessage ?: error.javaClass.simpleName)
            }
    }

    fun loadConversations(currentUid: String, onResult: (List<ConversationItem>) -> Unit) {
        db.collection("conversations")
            .whereArrayContains("participantUids", currentUid)
            .get()
            .addOnSuccessListener { conversations ->
                db.collection("users").document(currentUid)
                    .collection("messageSettings")
                    .get()
                    .addOnSuccessListener { settingsSnapshot ->
                        val settings = settingsSnapshot.documents.associateBy { it.id }
                        val items = conversations.documents.mapNotNull { document ->
                            val state = settings[document.id]
                            val hidden = state?.getBoolean("hidden") == true
                            val deleted = state?.getBoolean("deleted") == true
                            if (hidden || deleted) return@mapNotNull null

                            val participants = document.get("participantUids") as? List<*> ?: return@mapNotNull null
                            val otherUid = participants.filterIsInstance<String>().firstOrNull { it != currentUid }
                                ?: return@mapNotNull null
                            val names = document.get("participantNames") as? Map<*, *>
                            val otherName = names?.get(otherUid) as? String ?: "Gamer"
                            val timestamp = document.getTimestamp("updatedAt")
                            ConversationItem(
                                id = document.id,
                                name = otherName,
                                lastMessage = document.getString("lastMessage") ?: "",
                                timeFormatted = timestamp?.let { formatTimestamp(it) } ?: "Just now",
                                unreadCount = 0,
                                isGroup = participants.size > 2,
                                otherUid = otherUid,
                                archived = state?.getBoolean("archived") == true
                            )
                        }.sortedByDescending { document ->
                            conversations.documents.firstOrNull { it.id == document.id }
                                ?.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                        }
                        onResult(items)
                    }
                    .addOnFailureListener { error -> onResult(emptyList()) }
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun setConversationState(
        currentUid: String,
        conversationId: String,
        archived: Boolean,
        hidden: Boolean,
        deleted: Boolean = false,
        onResult: (Boolean) -> Unit
    ) {
        db.collection("users").document(currentUid)
            .collection("messageSettings").document(conversationId)
            .set(
                mapOf(
                    "archived" to archived,
                    "hidden" to hidden,
                    "deleted" to deleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun hideMessageForUser(currentUid: String, conversationId: String, messageId: String, onResult: (Boolean) -> Unit) {
        if (messageId.isBlank()) {
            onResult(false)
            return
        }
        db.collection("users").document(currentUid)
            .collection("messageSettings").document(conversationId)
            .set(mapOf("deletedMessageIds" to FieldValue.arrayUnion(messageId)), SetOptions.merge())
            .addOnSuccessListener {
                activeHiddenMessageIds = activeHiddenMessageIds + messageId
                onResult(true)
            }
            .addOnFailureListener { onResult(false) }
    }

    private fun formatTimestamp(timestamp: Timestamp): String {
        val diff = (System.currentTimeMillis() - timestamp.toDate().time).coerceAtLeast(0L)
        val minutes = diff / 60_000
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
}
