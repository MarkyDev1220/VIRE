package com.vire.android.android

object UserManager {

    private val demoUsers = listOf(
        "alice", "bob", "charlie", "dave", "eve", "frank", "grace",
        "heidi", "ivan", "judy", "mark", "olivia", "peggy", "trent",
        "victor", "walter"
    )

    private val users = mutableListOf<User>()
    private var nextId = 1L

    // Legacy username-only methods
    fun getAllUsernames(): List<String> = users.map { it.username } + demoUsers

    fun searchUsers(query: String, excludeUserId: Long? = null): List<User> {
        val q = query.trim().lowercase()
        return users.filter {
            (excludeUserId == null || it.id != excludeUserId) &&
                    it.username.lowercase().contains(q)
        }
    }

    fun userExists(username: String?): Boolean =
        username?.let { getAllUsernames().any { it.equals(username, ignoreCase = true) } } ?: false

    // Full user management
    fun addUser(user: User) {
        users.add(user.copy(id = nextId++))
    }

    fun updateUser(updated: User) {
        val index = users.indexOfFirst { it.id == updated.id }
        if (index >= 0) users[index] = updated
    }

    fun getUser(username: String): User? = users.find { it.username.equals(username, true) }
    fun getUserById(id: Long): User? = users.find { it.id == id }
    fun getAllUserObjects(): List<User> = users.toList()
    fun nextId(): Long = nextId++

    // Friends by ID
    fun addFriend(currentUserId: Long, friendId: Long) {
        val currentUser = getUserById(currentUserId)
        val friend = getUserById(friendId)
        if (currentUser != null && friend != null && currentUserId != friendId) {
            if (!currentUser.friends.contains(friendId)) currentUser.friends.add(friendId)
            if (!friend.friends.contains(currentUserId)) friend.friends.add(currentUserId)
            updateUser(currentUser)
            updateUser(friend)
        }
    }

    fun removeFriend(currentUserId: Long, friendId: Long) {
        val currentUser = getUserById(currentUserId)
        val friend = getUserById(friendId)
        currentUser?.friends?.remove(friendId)
        friend?.friends?.remove(currentUserId)
        if (currentUser != null) updateUser(currentUser)
        if (friend != null) updateUser(friend)
    }

    fun getFriends(userId: Long): List<User> =
        getUserById(userId)?.friends?.mapNotNull { getUserById(it) } ?: emptyList()

    fun getDeviceTokenForUser(username: String): String? = null
}
