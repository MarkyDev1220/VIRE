package com.example.vire.android

object UserManager {
    private val users = listOf(
        "alice", "bob", "charlie", "dave", "eve", "frank", "grace", "heidi", "ivan", "judy",
        "mark", "olivia", "peggy", "trent", "victor", "walter"
    )

    fun getAllUsers(): List<String> = users

    fun searchUsers(query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return users
        return users.filter { it.lowercase().contains(q) }
    }

    fun userExists(username: String?): Boolean {
        if (username == null) return false
        return users.any { it.equals(username.trim(), ignoreCase = true) }
    }


    fun getDeviceTokenForUser(username: String): String? {
        // In-memory demo — no tokens available.
        return null
    }
}