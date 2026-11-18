package com.vire.android.android

import com.google.firebase.firestore.FirebaseFirestore

object UserManager {

    private val users = mutableListOf<User>()

    fun syncFromFirestore(onComplete: (() -> Unit)? = null) {
        FirebaseFirestore.getInstance()
            .collection(UserDirectory.COLLECTION)
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<User>()
                for (doc in result) {
                    val uid = doc.id
                    val name = doc.getString("username")
                        ?: doc.getString("fullName")
                        ?: doc.getString("displayName")
                        ?: doc.getString("name")
                        ?: doc.getString("email")?.substringBefore("@")
                        ?: "Gamer"

                    val user = User(
                        id = uid,
                        username = name,
                        email = doc.getString("email") ?: doc.getString("emailAddress") ?: "",
                        gender = doc.getString("gender") ?: "",
                        dateOfBirth = doc.getString("dateOfBirth") ?: "",
                        favoriteGames = (doc.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        favoriteGenres = (doc.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        skillLevel = doc.getString("skillLevel") ?: "",
                        localArea = doc.getString("localArea") ?: "",
                        gamerBio = doc.getString("gamerBio") ?: ""
                    )
                    list.add(user)
                }
                setUsers(list)
                onComplete?.invoke()
            }
            .addOnFailureListener {
                onComplete?.invoke()
            }
    }

    // Legacy username-only methods
    fun getAllUsernames(): List<String> = users.map { it.username }

    fun searchUsers(query: String, excludeUserId: String? = null): List<User> {
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
        val index = users.indexOfFirst { it.id == user.id }
        if (index >= 0) {
            users[index] = user
        } else {
            users.add(user)
        }
    }

    fun setUsers(userList: List<User>) {
        users.clear()
        users.addAll(userList)
    }

    fun updateUser(updated: User) {
        val index = users.indexOfFirst { it.id == updated.id }
        if (index >= 0) users[index] = updated
    }

    fun getUser(username: String): User? = users.find { it.username.equals(username, true) }
    fun getUserById(id: String): User? = users.find { it.id == id }
    fun getAllUserObjects(): List<User> = users.toList()

    // Friends by ID
    fun addFriend(currentUserId: String, friendId: String) {
        val currentUser = getUserById(currentUserId)
        val friend = getUserById(friendId)
        if (currentUser != null && friend != null && currentUserId != friendId) {
            if (!currentUser.friends.contains(friendId)) currentUser.friends.add(friendId)
            if (!friend.friends.contains(currentUserId)) friend.friends.add(currentUserId)
            updateUser(currentUser)
            updateUser(friend)
        }
    }

    fun removeFriend(currentUserId: String, friendId: String) {
        val currentUser = getUserById(currentUserId)
        val friend = getUserById(friendId)
        currentUser?.friends?.remove(friendId)
        friend?.friends?.remove(currentUserId)
        if (currentUser != null) updateUser(currentUser)
        if (friend != null) updateUser(friend)
    }

    fun getFriends(userId: String): List<User> =
        getUserById(userId)?.friends?.mapNotNull { getUserById(it) } ?: emptyList()

    fun getDeviceTokenForUser(username: String): String? = null
}
