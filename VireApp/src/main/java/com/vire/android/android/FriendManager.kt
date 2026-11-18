package com.vire.android.android

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

object FriendManager {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun currentUid(): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    /** Send a friend request from current user -> receiverUid */
    fun sendRequest(receiverUid: String, onResult: (Boolean) -> Unit) {
        val senderUid = currentUid() ?: return onResult(false)
        if (senderUid == receiverUid) return onResult(false)

        // Check if already friends or if request already exists
        isFriend(receiverUid) { alreadyFriends ->
            if (alreadyFriends) return@isFriend onResult(false)

            db.collection("friendRequests")
                .whereEqualTo("fromUid", senderUid)
                .whereEqualTo("toUid", receiverUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        // Request already pending
                        onResult(false)
                    } else {
                        val data = hashMapOf(
                            "fromUid" to senderUid,
                            "toUid" to receiverUid,
                            "status" to "pending",
                            "createdAt" to Timestamp.now()
                        )

                        db.collection("friendRequests")
                            .add(data)
                            .addOnSuccessListener {
                                dispatchFriendRequestNotification(senderUid, receiverUid)
                                onResult(true)
                            }
                            .addOnFailureListener { onResult(true) }
                    }
                }
                .addOnFailureListener {
                    val data = hashMapOf(
                        "fromUid" to senderUid,
                        "toUid" to receiverUid,
                        "status" to "pending",
                        "createdAt" to Timestamp.now()
                    )
                    db.collection("friendRequests").add(data)
                        .addOnSuccessListener {
                            dispatchFriendRequestNotification(senderUid, receiverUid)
                            onResult(true)
                        }
                        .addOnFailureListener { onResult(true) }
                }
        }
    }

    private fun dispatchFriendRequestNotification(senderUid: String, receiverUid: String) {
        db.collection("users").document(senderUid).get()
            .addOnSuccessListener { doc ->
                val senderName = doc.getString("username")
                    ?: doc.getString("fullName")
                    ?: doc.getString("displayName")
                    ?: "A gamer"

                NotificationHelper.sendNotification(
                    toUid = receiverUid,
                    fromUid = senderUid,
                    fromUsername = senderName,
                    title = "New Friend Request",
                    message = "$senderName sent you a friend request!",
                    type = "FRIEND_REQUEST"
                )
            }
            .addOnFailureListener {
                NotificationHelper.sendNotification(
                    toUid = receiverUid,
                    fromUid = senderUid,
                    fromUsername = "A gamer",
                    title = "New Friend Request",
                    message = "Someone sent you a friend request!",
                    type = "FRIEND_REQUEST"
                )
            }
    }

    /** Send a friend request by searching username or email */
    fun sendRequestByUsername(usernameOrEmail: String, onResult: (Boolean, String) -> Unit) {
        val queryStr = usernameOrEmail.trim()
        if (queryStr.isEmpty()) {
            onResult(false, "Please enter a username or email.")
            return
        }

        val senderUid = currentUid()
        if (senderUid == null) {
            onResult(false, "You must be logged in to send friend requests.")
            return
        }

        findUserByQuery(queryStr) { matchedUser ->
            if (matchedUser == null) {
                onResult(false, "No user found matching '$queryStr'.")
                return@findUserByQuery
            }

            if (matchedUser.id == senderUid) {
                onResult(false, "You cannot send a friend request to yourself.")
                return@findUserByQuery
            }

            sendRequest(matchedUser.id) { success ->
                if (success) {
                    onResult(true, "Friend request sent to ${matchedUser.username}!")
                } else {
                    onResult(false, "Request already sent or user is already your friend.")
                }
            }
        }
    }

    /** Fast targeted search for a user by username or email */
    fun findUserByQuery(query: String, onResult: (User?) -> Unit) {
        val qStr = query.trim()
        if (qStr.isEmpty()) {
            onResult(null)
            return
        }

        val qLower = qStr.lowercase()

        // 1. Exact match on usernameLowercase
        db.collection("users")
            .whereEqualTo("usernameLowercase", qLower)
            .get()
            .addOnSuccessListener { snap1 ->
                if (!snap1.isEmpty) {
                    onResult(parseUserDoc(snap1.documents.first()))
                    return@addOnSuccessListener
                }

                // 2. Exact match on username
                db.collection("users")
                    .whereEqualTo("username", qStr)
                    .get()
                    .addOnSuccessListener { snap2 ->
                        if (!snap2.isEmpty) {
                            onResult(parseUserDoc(snap2.documents.first()))
                            return@addOnSuccessListener
                        }

                        // 3. Exact match on email
                        db.collection("users")
                            .whereEqualTo("email", qLower)
                            .get()
                            .addOnSuccessListener { snap3 ->
                                if (!snap3.isEmpty) {
                                    onResult(parseUserDoc(snap3.documents.first()))
                                    return@addOnSuccessListener
                                }

                                // 4. Prefix match on usernameLowercase
                                db.collection("users")
                                    .whereGreaterThanOrEqualTo("usernameLowercase", qLower)
                                    .whereLessThanOrEqualTo("usernameLowercase", qLower + "\uf8ff")
                                    .get()
                                    .addOnSuccessListener { snap4 ->
                                        if (!snap4.isEmpty) {
                                            onResult(parseUserDoc(snap4.documents.first()))
                                            return@addOnSuccessListener
                                        }

                                        // 5. Broad collection scan in memory
                                        db.collection("users").get()
                                            .addOnSuccessListener { snap5 ->
                                                val matchedDoc = snap5.documents.find { doc ->
                                                    val u = doc.getString("username") ?: ""
                                                    val uLower = doc.getString("usernameLowercase") ?: u.lowercase()
                                                    val e = doc.getString("email") ?: doc.getString("emailAddress") ?: ""
                                                    val fullName = doc.getString("fullName") ?: doc.getString("displayName") ?: doc.getString("name") ?: ""

                                                    u.equals(qStr, ignoreCase = true) ||
                                                            uLower == qLower ||
                                                            uLower.contains(qLower) ||
                                                            e.equals(qStr, ignoreCase = true) ||
                                                            e.lowercase().contains(qLower) ||
                                                            fullName.equals(qStr, ignoreCase = true) ||
                                                            fullName.lowercase().contains(qLower)
                                                }

                                                if (matchedDoc != null) {
                                                    val user = parseUserDoc(matchedDoc)
                                                    if (!matchedDoc.contains("usernameLowercase") && user.username.isNotBlank()) {
                                                        db.collection("users").document(user.id)
                                                            .update("usernameLowercase", user.username.lowercase())
                                                    }
                                                    onResult(user)
                                                } else {
                                                    onResult(null)
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                android.util.Log.e("FriendManager", "Error in broad collection scan: ${e.message}", e)
                                                onResult(null)
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        android.util.Log.e("FriendManager", "Error in prefix query: ${e.message}", e)
                                        onResult(null)
                                    }
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("FriendManager", "Error in email query: ${e.message}", e)
                                onResult(null)
                            }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FriendManager", "Error in username query: ${e.message}", e)
                        onResult(null)
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FriendManager", "Error in usernameLowercase query: ${e.message}", e)
                onResult(null)
            }
    }

    private fun parseUserDoc(doc: com.google.firebase.firestore.DocumentSnapshot): User {
        val uid = doc.id
        val email = doc.getString("email")
            ?: doc.getString("emailAddress")
            ?: ""
        val rawName = doc.getString("username")
            ?: doc.getString("fullName")
            ?: doc.getString("displayName")
            ?: doc.getString("name")
        val name = when {
            !rawName.isNullOrBlank() -> rawName
            email.isNotBlank() -> email.substringBefore("@")
            else -> "User (${uid.take(6)})"
        }
        return User(
            id = uid,
            username = name,
            email = email,
            gender = doc.getString("gender") ?: "",
            dateOfBirth = doc.getString("dateOfBirth") ?: "",
            favoriteGames = (doc.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            favoriteGenres = (doc.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            skillLevel = doc.getString("skillLevel") ?: "",
            localArea = doc.getString("localArea") ?: "",
            gamerBio = doc.getString("gamerBio") ?: ""
        )
    }

    /** Accept a friend request and create friendship */
    fun acceptRequest(requestId: String, onResult: (Boolean) -> Unit) {
        val reqRef = db.collection("friendRequests").document(requestId)
        db.runTransaction { tx ->
            val snap = tx.get(reqRef)
            val fromUid = snap.getString("fromUid") ?: return@runTransaction
            val toUid = snap.getString("toUid") ?: return@runTransaction

            tx.update(reqRef, "status", "accepted")

            val friendsRef = db.collection("friends").document()
            tx.set(friendsRef, hashMapOf(
                "userA" to fromUid,
                "userB" to toUid,
                "createdAt" to Timestamp.now()
            ))
        }.addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /** Decline a friend request */
    fun declineRequest(requestId: String, onResult: (Boolean) -> Unit) {
        db.collection("friendRequests").document(requestId)
            .update("status", "declined")
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /** Remove friendship between two users */
    fun removeFriend(otherUid: String, onResult: (Boolean) -> Unit) {
        val uid = currentUid() ?: return onResult(false)
        db.collection("friends")
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                var found = false
                for (doc in result) {
                    val a = doc.getString("userA")
                    val b = doc.getString("userB")
                    if ((a == uid && b == otherUid) || (a == otherUid && b == uid)) {
                        batch.delete(doc.reference)
                        found = true
                    }
                }
                if (found) {
                    batch.commit()
                        .addOnSuccessListener { onResult(true) }
                        .addOnFailureListener { onResult(false) }
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener { onResult(false) }
    }

    /** Check if current user is friends with otherUid */
    fun isFriend(otherUid: String, onResult: (Boolean) -> Unit) {
        val uid = currentUid() ?: return onResult(false)
        db.collection("friends")
            .get()
            .addOnSuccessListener { result ->
                var found = false
                for (doc in result) {
                    val a = doc.getString("userA")
                    val b = doc.getString("userB")
                    if ((a == uid && b == otherUid) || (a == otherUid && b == uid)) {
                        found = true
                        break
                    }
                }
                onResult(found)
            }
            .addOnFailureListener { onResult(false) }
    }

    /** Get all friends of current user */
    fun getFriends(onResult: (List<String>) -> Unit) {
        val uid = currentUid() ?: return onResult(emptyList())
        db.collection("friends")
            .get()
            .addOnSuccessListener { result ->
                val friends = mutableListOf<String>()
                for (doc in result) {
                    val a = doc.getString("userA")
                    val b = doc.getString("userB")
                    if (a == uid && !b.isNullOrEmpty()) friends.add(b)
                    else if (b == uid && !a.isNullOrEmpty()) friends.add(a)
                }
                onResult(friends)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    /** Get pending friend requests for current user */
    fun getPendingRequests(onResult: (List<Pair<String, String>>) -> Unit) {
        val uid = currentUid() ?: return onResult(emptyList())
        db.collection("friendRequests")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    val from = doc.getString("fromUid") ?: return@mapNotNull null
                    Pair(doc.id, from)
                }
                onResult(list)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    enum class FriendshipState {
        NONE,
        PENDING_SENT,
        PENDING_RECEIVED,
        FRIENDS
    }

    fun getFriendshipState(otherUid: String, onResult: (FriendshipState, String?) -> Unit) {
        val uid = currentUid() ?: return onResult(FriendshipState.NONE, null)
        if (uid == otherUid) return onResult(FriendshipState.NONE, null)

        isFriend(otherUid) { isFr ->
            if (isFr) {
                onResult(FriendshipState.FRIENDS, null)
                return@isFriend
            }

            db.collection("friendRequests")
                .whereEqualTo("fromUid", uid)
                .whereEqualTo("toUid", otherUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { sentSnap ->
                    if (!sentSnap.isEmpty) {
                        val reqId = sentSnap.documents.firstOrNull()?.id
                        onResult(FriendshipState.PENDING_SENT, reqId)
                    } else {
                        db.collection("friendRequests")
                            .whereEqualTo("fromUid", otherUid)
                            .whereEqualTo("toUid", uid)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener { recvSnap ->
                                if (!recvSnap.isEmpty) {
                                    val reqId = recvSnap.documents.firstOrNull()?.id
                                    onResult(FriendshipState.PENDING_RECEIVED, reqId)
                                } else {
                                    onResult(FriendshipState.NONE, null)
                                }
                            }
                            .addOnFailureListener { onResult(FriendshipState.NONE, null) }
                    }
                }
                .addOnFailureListener { onResult(FriendshipState.NONE, null) }
        }
    }
}
