package com.vire.android.android

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

object FriendManager {
    private const val TAG = "FriendManager"

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun currentUid(): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    /** Send a friend request from the current user to receiverUid. */
    fun sendRequest(receiverUid: String, onResult: (Boolean) -> Unit) {
        sendFriendRequest(receiverUid) { success, _ -> onResult(success) }
    }

    fun sendFriendRequest(receiverUid: String, onResult: (Boolean, String) -> Unit) {
        val senderUid = currentUid()
        if (senderUid == null) {
            onResult(false, "Sign in to send friend requests.")
            return
        }
        if (receiverUid.isBlank() || senderUid == receiverUid) {
            onResult(false, "You cannot send a friend request to this user.")
            return
        }

        TrustSafetyManager.isBlocked(receiverUid) { blocked ->
            if (blocked) {
                onResult(false, "This user is blocked or their block status could not be checked.")
                return@isBlocked
            }

            isFriend(receiverUid) { alreadyFriends ->
                if (alreadyFriends) {
                    onResult(false, "You are already friends with this user.")
                    return@isFriend
                }

                val requests = db.collection("friendRequests")
                requests.whereEqualTo("fromUid", senderUid)
                    .whereEqualTo("toUid", receiverUid)
                    .whereEqualTo("status", "pending")
                    .get()
                    .addOnSuccessListener { outgoing ->
                        if (!outgoing.isEmpty) {
                            onResult(false, "Your friend request is already pending.")
                            return@addOnSuccessListener
                        }

                        requests.whereEqualTo("fromUid", receiverUid)
                            .whereEqualTo("toUid", senderUid)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener { incoming ->
                                if (!incoming.isEmpty) {
                                    onResult(false, "This user already sent you a request. Accept or decline it in Friends.")
                                    return@addOnSuccessListener
                                }

                                val request = hashMapOf(
                                    "fromUid" to senderUid,
                                    "toUid" to receiverUid,
                                    "status" to "pending",
                                    "createdAt" to Timestamp.now()
                                )
                                requests.add(request)
                                    .addOnSuccessListener {
                                        dispatchFriendRequestNotification(senderUid, receiverUid)
                                        onResult(true, "Friend request sent.")
                                    }
                                    .addOnFailureListener { error ->
                                        Log.e(TAG, "Could not send friend request to $receiverUid", error)
                                        onResult(false, "Could not send friend request: ${error.localizedMessage ?: "check your connection and try again"}")
                                    }
                            }
                            .addOnFailureListener { error ->
                                Log.e(TAG, "Could not check incoming friend requests for $senderUid", error)
                                onResult(false, "Could not check for existing requests: ${error.localizedMessage ?: "try again"}")
                            }
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Could not check outgoing friend requests for $senderUid", error)
                        onResult(false, "Could not check for an existing request: ${error.localizedMessage ?: "try again"}")
                    }
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

            sendFriendRequest(matchedUser.id) { success, message ->
                onResult(success, if (success) "Friend request sent to ${matchedUser.username}!" else message)
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

        // 1. Try exact match on username / usernameLowercase
        db.collection("users")
            .whereEqualTo("usernameLowercase", qLower)
            .get()
            .addOnSuccessListener { snap1 ->
                if (!snap1.isEmpty) {
                    val doc = snap1.documents.first()
                    onResult(parseUserDoc(doc))
                    return@addOnSuccessListener
                }

                // 2. Try match on email
                db.collection("users")
                    .whereEqualTo("email", qLower)
                    .get()
                    .addOnSuccessListener { snap2 ->
                        if (!snap2.isEmpty) {
                            val doc = snap2.documents.first()
                            onResult(parseUserDoc(doc))
                            return@addOnSuccessListener
                        }

                        // 3. Try prefix range query on usernameLowercase
                        db.collection("users")
                            .whereGreaterThanOrEqualTo("usernameLowercase", qLower)
                            .whereLessThanOrEqualTo("usernameLowercase", qLower + "\uf8ff")
                            .limit(1)
                            .get()
                            .addOnSuccessListener { snap3 ->
                                if (!snap3.isEmpty) {
                                    val doc = snap3.documents.first()
                                    onResult(parseUserDoc(doc))
                                    return@addOnSuccessListener
                                }

                                // 4. Fallback memory search across full collection
                                db.collection("users").get()
                                    .addOnSuccessListener { allDocs ->
                                        val matched = allDocs.documents.find { doc ->
                                            val u = doc.getString("username") ?: ""
                                            val e = doc.getString("email") ?: ""
                                            val name = doc.getString("fullName") ?: doc.getString("displayName") ?: ""
                                            u.equals(qStr, ignoreCase = true) ||
                                                    e.equals(qStr, ignoreCase = true) ||
                                                    name.equals(qStr, ignoreCase = true) ||
                                                    u.lowercase().contains(qLower)
                                        }
                                        if (matched != null) {
                                            onResult(parseUserDoc(matched))
                                        } else {
                                            onResult(findDemoUser(qStr))
                                        }
                                    }
                                    .addOnFailureListener { onResult(findDemoUser(qStr)) }
                            }
                            .addOnFailureListener { onResult(findDemoUser(qStr)) }
                    }
                    .addOnFailureListener { onResult(findDemoUser(qStr)) }
            }
            .addOnFailureListener { onResult(findDemoUser(qStr)) }
    }

    fun getDemoUsers(): List<User> {
        return listOf(
            User(
                id = "demo_alex",
                username = "Alex_Gamer",
                email = "alex@example.com",
                favoriteGames = listOf("Magic: The Gathering", "Lorcana"),
                skillLevel = "Intermediate",
                localArea = "Downtown",
                gamerBio = "Avid MTG Commander player!"
            ),
            User(
                id = "demo_sam",
                username = "Sam_TCG",
                email = "sam@example.com",
                favoriteGames = listOf("Pokemon TCG", "Yu-Gi-Oh!"),
                skillLevel = "Advanced",
                localArea = "Uptown",
                gamerBio = "Competitive Pokemon TCG player."
            ),
            User(
                id = "demo_jordan",
                username = "Jordan_Deckmaster",
                email = "jordan@example.com",
                favoriteGames = listOf("Magic: The Gathering", "Yu-Gi-Oh!"),
                skillLevel = "Expert",
                localArea = "Westside",
                gamerBio = "Looking for local casual matches."
            )
        )
    }

    private fun findDemoUser(queryStr: String): User? {
        val qStr = queryStr.trim()
        val qLower = qStr.lowercase()
        return getDemoUsers().find { user ->
            user.username.equals(qStr, ignoreCase = true) ||
                    user.email.equals(qStr, ignoreCase = true) ||
                    user.username.lowercase().contains(qLower)
        }
    }

    private fun parseUserDoc(doc: com.google.firebase.firestore.DocumentSnapshot): User {
        val uid = doc.id
        val name = doc.getString("username")
            ?: doc.getString("fullName")
            ?: doc.getString("displayName")
            ?: "Gamer"
        return User(
            id = uid,
            username = name,
            email = doc.getString("email") ?: "",
            gender = doc.getString("gender") ?: "",
            dateOfBirth = doc.getString("dateOfBirth") ?: "",
            favoriteGames = (doc.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            favoriteGenres = (doc.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            skillLevel = doc.getString("skillLevel") ?: "",
            localArea = doc.getString("localArea") ?: "",
            gamerBio = doc.getString("gamerBio") ?: ""
        )
    }

    /** Accept a pending request addressed to the signed-in user and create the friendship. */
    fun acceptRequest(requestId: String, onResult: (Boolean) -> Unit) {
        val uid = currentUid()
        if (uid == null || requestId.isBlank()) {
            onResult(false)
            return
        }

        val requestRef = db.collection("friendRequests").document(requestId)
        db.runTransaction { transaction ->
            val request = transaction.get(requestRef)
            val fromUid = request.getString("fromUid")
            val toUid = request.getString("toUid")
            check(request.exists() && request.getString("status") == "pending" && toUid == uid && !fromUid.isNullOrBlank()) {
                "This friend request is no longer available."
            }
            val requesterUid: String = fromUid

            val firstUid = minOf(uid, requesterUid)
            val secondUid = maxOf(uid, requesterUid)
            val friendshipRef = db.collection("friends").document("$firstUid:$secondUid")
            val friendship = transaction.get(friendshipRef)

            transaction.update(requestRef, "status", "accepted")
            if (!friendship.exists()) {
                transaction.set(friendshipRef, hashMapOf(
                    "userA" to requesterUid,
                    "userB" to uid,
                    "createdAt" to Timestamp.now()
                ))
            }
        }.addOnSuccessListener { onResult(true) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Could not accept friend request $requestId", error)
                onResult(false)
            }
    }

    /** Decline a pending request addressed to the signed-in user. */
    fun declineRequest(requestId: String, onResult: (Boolean) -> Unit) {
        val uid = currentUid()
        if (uid == null || requestId.isBlank()) {
            onResult(false)
            return
        }

        val requestRef = db.collection("friendRequests").document(requestId)
        db.runTransaction { transaction ->
            val request = transaction.get(requestRef)
            check(
                request.exists() && request.getString("status") == "pending" &&
                    request.getString("toUid") == uid
            ) { "This friend request is no longer available." }
            transaction.update(requestRef, "status", "declined")
        }.addOnSuccessListener { onResult(true) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Could not decline friend request $requestId", error)
                onResult(false)
            }
    }

    private fun loadFriendshipDocuments(
        uid: String,
        onResult: (List<com.google.firebase.firestore.DocumentSnapshot>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val friends = db.collection("friends")
        friends.whereEqualTo("userA", uid)
            .get()
            .addOnSuccessListener { outgoing ->
                friends.whereEqualTo("userB", uid)
                    .get()
                    .addOnSuccessListener { incoming ->
                        onResult((outgoing.documents + incoming.documents).distinctBy { it.id })
                    }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }

    /** Remove friendship between two users. */
    fun removeFriend(otherUid: String, onResult: (Boolean) -> Unit) {
        val uid = currentUid()
        if (uid == null || otherUid.isBlank() || uid == otherUid) {
            onResult(false)
            return
        }

        loadFriendshipDocuments(uid, { documents ->
            val matching = documents.filter { document ->
                (document.getString("userA") == uid && document.getString("userB") == otherUid) ||
                    (document.getString("userA") == otherUid && document.getString("userB") == uid)
            }
            if (matching.isEmpty()) {
                onResult(false)
            } else {
                val batch = db.batch()
                matching.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Could not remove friendship with $otherUid", error)
                        onResult(false)
                    }
            }
        }, { error ->
            Log.e(TAG, "Could not load friendship with $otherUid", error)
            onResult(false)
        })
    }

    /** Check if current user is friends with otherUid. */
    fun isFriend(otherUid: String, onResult: (Boolean) -> Unit) {
        val uid = currentUid()
        if (uid == null || otherUid.isBlank() || uid == otherUid) {
            onResult(false)
            return
        }
        loadFriendshipDocuments(uid, { documents ->
            onResult(documents.any { document ->
                (document.getString("userA") == uid && document.getString("userB") == otherUid) ||
                    (document.getString("userA") == otherUid && document.getString("userB") == uid)
            })
        }, { error ->
            Log.e(TAG, "Could not check friendship with $otherUid", error)
            onResult(false)
        })
    }

    /** Get all friends of current user. */
    fun getFriends(onResult: (List<String>) -> Unit) {
        val uid = currentUid() ?: return onResult(emptyList())
        loadFriendshipDocuments(uid, { documents ->
            onResult(documents.mapNotNull { document ->
                val a = document.getString("userA")
                val b = document.getString("userB")
                when (uid) {
                    a -> b?.takeIf { it.isNotBlank() }
                    b -> a?.takeIf { it.isNotBlank() }
                    else -> null
                }
            }.distinct())
        }, { error ->
            Log.e(TAG, "Could not load friends for $uid", error)
            onResult(emptyList())
        })
    }

    /** Get pending friend requests addressed to the signed-in user. */
    fun getPendingRequests(onResult: (List<Pair<String, String>>, String?) -> Unit) {
        val uid = currentUid()
        if (uid == null) {
            onResult(emptyList(), "Sign in to view friend requests.")
            return
        }
        db.collection("friendRequests")
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                val requests = result.documents.mapNotNull { document ->
                    val fromUid = document.getString("fromUid") ?: return@mapNotNull null
                    document.id to fromUid
                }
                onResult(requests, null)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Could not load pending friend requests for $uid", error)
                onResult(emptyList(), error.localizedMessage ?: "Could not load friend requests.")
            }
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
