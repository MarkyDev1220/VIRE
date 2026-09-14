package com.vire.android.android

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

object FriendManager {

    private val db = FirebaseFirestore.getInstance()

    private fun currentUid(): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    /** Send a friend request from current user -> receiverUid */
    fun sendRequest(receiverUid: String, onResult: (Boolean) -> Unit) {
        val senderUid = currentUid() ?: return onResult(false)
        if (senderUid == receiverUid) return onResult(false)

        val data = hashMapOf(
            "fromUid" to senderUid,
            "toUid" to receiverUid,
            "status" to "pending",
            "createdAt" to Timestamp.now()
        )

        db.collection("friendRequests")
            .add(data)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
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
            .whereIn("userA", listOf(uid, otherUid))
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                for (doc in result) {
                    val a = doc.getString("userA")
                    val b = doc.getString("userB")
                    if ((a == uid && b == otherUid) || (a == otherUid && b == uid)) {
                        batch.delete(doc.reference)
                    }
                }
                batch.commit()
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener { onResult(false) }
    }

    /** Check if current user is friends with otherUid */
    fun isFriend(otherUid: String, onResult: (Boolean) -> Unit) {
        val uid = currentUid() ?: return onResult(false)
        db.collection("friends")
            .whereIn("userA", listOf(uid, otherUid))
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
            .whereIn("userA", listOf(uid))
            .get()
            .addOnSuccessListener { result ->
                val friends = mutableListOf<String>()
                for (doc in result) {
                    val a = doc.getString("userA")
                    val b = doc.getString("userB")
                    if (a == uid && !b.isNullOrEmpty()) friends.add(b)
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
}
