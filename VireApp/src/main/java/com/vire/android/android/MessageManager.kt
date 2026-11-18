package com.vire.android.android

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object MessageManager {
    private const val TAG = "MessageManager"
    private const val PREFERENCES_PREFIX = "message_store_"

    private val messagesMap = mutableMapOf<String, MutableList<Message>>()
    private val unreadMap = mutableMapOf<String, Int>()
    private val groupSet = mutableSetOf<String>()
    private val archivedSet = mutableSetOf<String>()
    private val hiddenSet = mutableSetOf<String>()
    private val timeLabelMap = mutableMapOf<String, String>()
    private val allUsers = mutableSetOf<String>()

    private var preferences: SharedPreferences? = null
    private var activeAccount: String? = null

    @Synchronized
    fun initialize(context: Context) {
        val account = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        if (activeAccount == account) return

        clearMemory()
        activeAccount = account
        preferences = context.applicationContext.getSharedPreferences(
            "$PREFERENCES_PREFIX$account",
            Context.MODE_PRIVATE
        )
        loadPersistedState()
    }

    @Synchronized
    fun clearAll() {
        clearMemory()
        persist()
    }

    @Synchronized
    fun setRegisteredUsers(usernames: List<String>) {
        allUsers.clear()
        allUsers.addAll(usernames.filter { it.isNotBlank() })
        persist()
    }

    @Synchronized
    fun registerUser(username: String) {
        if (username.isNotBlank()) {
            allUsers.add(username)
            messagesMap.getOrPut(username) { mutableListOf() }
            persist()
        }
    }

    fun getMessagesForUser(user: String): MutableList<Message> =
        messagesMap.getOrPut(user) { mutableListOf() }

    @Synchronized
    fun sendMessageForUser(user: String, message: Message) {
        messagesMap.getOrPut(user) { mutableListOf() }.add(message)
        allUsers.add(user)
        unreadMap[user] = 0
        timeLabelMap[user] = "Just now"
        archivedSet.remove(user)
        hiddenSet.remove(user)
        persist()
    }

    @Synchronized
    fun receiveMessageForUser(user: String, message: Message) {
        messagesMap.getOrPut(user) { mutableListOf() }.add(message)
        allUsers.add(user)
        unreadMap[user] = (unreadMap[user] ?: 0) + 1
        timeLabelMap[user] = "Just now"
        archivedSet.remove(user)
        persist()
    }

    @Synchronized
    fun markAsRead(user: String) {
        unreadMap[user] = 0
        persist()
    }

    @Synchronized
    fun markAllAsRead() {
        messagesMap.keys.forEach { unreadMap[it] = 0 }
        persist()
    }

    fun getTotalUnreadCount(): Int = unreadMap.values.sum()

    fun getUnreadCount(user: String): Int = unreadMap[user] ?: 0

    @Synchronized
    fun setConversationArchived(user: String, archived: Boolean) {
        if (archived) {
            archivedSet.add(user)
            hiddenSet.remove(user)
        } else {
            archivedSet.remove(user)
        }
        persist()
    }

    @Synchronized
    fun setConversationHidden(user: String, hidden: Boolean) {
        if (hidden) {
            hiddenSet.add(user)
            archivedSet.remove(user)
        } else {
            hiddenSet.remove(user)
        }
        persist()
    }

    fun isConversationArchived(user: String): Boolean = user in archivedSet

    fun isConversationHidden(user: String): Boolean = user in hiddenSet

    @Synchronized
    fun deleteConversation(user: String) {
        messagesMap.remove(user)
        unreadMap.remove(user)
        groupSet.remove(user)
        archivedSet.remove(user)
        hiddenSet.remove(user)
        timeLabelMap.remove(user)
        persist()
    }

    fun getConversationsList(): List<ConversationItem> {
        return messagesMap
            .filter { (_, messages) -> messages.isNotEmpty() }
            .map { (user, messages) ->
                val lastMessage = messages.last()
                ConversationItem(
                    id = user,
                    name = user,
                    lastMessage = lastMessage.content,
                    timeFormatted = timeLabelMap[user] ?: formatTimestamp(lastMessage.timestamp),
                    unreadCount = unreadMap[user] ?: 0,
                    isGroup = user in groupSet,
                    isArchived = user in archivedSet,
                    isHidden = user in hiddenSet
                )
            }
            .sortedWith(compareByDescending<ConversationItem> { it.unreadCount > 0 }.thenBy { it.name })
    }

    private fun formatTimestamp(timestamp: Long): String {
        val seconds = (System.currentTimeMillis() - timestamp) / 1000
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

    fun getLastMessagesForAllUsers(): List<Pair<String, Message?>> = messagesMap.map { (user, messages) ->
        user to messages.lastOrNull()
    }

    fun getAllUsersAsMessageList(): List<Pair<String, Message?>> = allUsers.map { user ->
        user to messagesMap[user]?.lastOrNull()
    }

    fun getAllUsernames(): List<String> = allUsers.toList()

    private fun clearMemory() {
        messagesMap.clear()
        unreadMap.clear()
        groupSet.clear()
        archivedSet.clear()
        hiddenSet.clear()
        timeLabelMap.clear()
        allUsers.clear()
    }

    private fun loadPersistedState() {
        val storedState = preferences?.getString("state", null) ?: return
        try {
            val state = JSONObject(storedState)
            val storedMessages = state.optJSONObject("messages") ?: JSONObject()
            val users = storedMessages.keys()
            while (users.hasNext()) {
                val user = users.next()
                val storedUserMessages = storedMessages.optJSONArray(user) ?: continue
                val messages = mutableListOf<Message>()
                for (index in 0 until storedUserMessages.length()) {
                    val item = storedUserMessages.optJSONObject(index) ?: continue
                    val sender = item.optString("sender")
                    val content = item.optString("content")
                    if (sender.isNotBlank() && content.isNotBlank()) {
                        messages.add(Message(sender, content, item.optLong("timestamp")))
                    }
                }
                if (messages.isNotEmpty()) messagesMap[user] = messages
            }

            val unread = state.optJSONObject("unread") ?: JSONObject()
            val unreadUsers = unread.keys()
            while (unreadUsers.hasNext()) {
                val user = unreadUsers.next()
                unreadMap[user] = unread.optInt(user).coerceAtLeast(0)
            }
            readStringArray(state.optJSONArray("groups"), groupSet)
            readStringArray(state.optJSONArray("archived"), archivedSet)
            readStringArray(state.optJSONArray("hidden"), hiddenSet)
            readStringArray(state.optJSONArray("users"), allUsers)

            val times = state.optJSONObject("times") ?: JSONObject()
            val timeUsers = times.keys()
            while (timeUsers.hasNext()) {
                val user = timeUsers.next()
                timeLabelMap[user] = times.optString(user)
            }
        } catch (error: JSONException) {
            Log.e(TAG, "Could not restore saved messages", error)
        }
    }

    private fun readStringArray(array: JSONArray?, destination: MutableSet<String>) {
        if (array == null) return
        for (index in 0 until array.length()) {
            array.optString(index).takeIf { it.isNotBlank() }?.let(destination::add)
        }
    }

    private fun persist() {
        val editor = preferences?.edit() ?: return
        try {
            val state = JSONObject()
            val messages = JSONObject()
            messagesMap.forEach { (user, userMessages) ->
                val items = JSONArray()
                userMessages.forEach { message ->
                    items.put(JSONObject()
                        .put("sender", message.sender)
                        .put("content", message.content)
                        .put("timestamp", message.timestamp))
                }
                messages.put(user, items)
            }
            state.put("messages", messages)

            val unread = JSONObject()
            unreadMap.forEach { (user, count) -> unread.put(user, count) }
            state.put("unread", unread)
            state.put("groups", JSONArray(groupSet.toList()))
            state.put("archived", JSONArray(archivedSet.toList()))
            state.put("hidden", JSONArray(hiddenSet.toList()))
            state.put("users", JSONArray(allUsers.toList()))

            val times = JSONObject()
            timeLabelMap.forEach { (user, label) -> times.put(user, label) }
            state.put("times", times)
            editor.putString("state", state.toString()).apply()
        } catch (error: JSONException) {
            Log.e(TAG, "Could not save messages", error)
        }
    }
}
