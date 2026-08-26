package com.vire.android.android

object FriendManager {

    // Map each user to their set of friends
    private val friendsMap = mutableMapOf<String, MutableSet<String>>()

    // Map each user to incoming friend requests
    private val friendRequestsMap = mutableMapOf<String, MutableSet<String>>()

    /** Send a friend request from sender -> receiver */
    fun sendRequest(sender: String, receiver: String) {
        val requests = friendRequestsMap.getOrPut(receiver) { mutableSetOf() }
        if (receiver != sender && !isFriend(sender, receiver)) {
            requests.add(sender)
        }
    }

    /** Accept a friend request for user from sender */
    fun acceptRequest(user: String, sender: String) {
        friendRequestsMap[user]?.remove(sender)
        addFriend(user, sender)
        addFriend(sender, user)
    }

    /** Reject a friend request for user from sender */
    fun rejectRequest(user: String, sender: String) {
        friendRequestsMap[user]?.remove(sender)
    }

    /** Check if two users are already friends */
    fun isFriend(user1: String, user2: String): Boolean {
        return friendsMap[user1]?.contains(user2) == true
    }

    /** Get all friends of a user */
    fun getFriends(user: String): Set<String> {
        return friendsMap[user] ?: emptySet()
    }

    /** Get all pending friend requests for a user */
    fun getFriendRequests(user: String): Set<String> {
        return friendRequestsMap[user] ?: emptySet()
    }

    /** Private helper to add friends */
    private fun addFriend(user: String, friend: String) {
        val set = friendsMap.getOrPut(user) { mutableSetOf() }
        set.add(friend)
    }

    /** Remove a friend from both sides */
    fun removeFriend(user: String, friend: String) {
        friendsMap[user]?.remove(friend)
        friendsMap[friend]?.remove(user)
    }
}
