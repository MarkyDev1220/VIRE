package com.vire.android.android

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class BlockedUser(val uid: String, val username: String)

object TrustSafetyManager {
    private const val TAG = "TrustSafetyManager"
    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun currentUid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun getBlockedUsers(onLoaded: (List<BlockedUser>) -> Unit) {
        val uid = currentUid()
        if (uid == null) {
            onLoaded(emptyList())
            return
        }

        db.collection("users").document(uid).collection("blockedUsers")
            .get()
            .addOnSuccessListener { snapshot ->
                onLoaded(snapshot.documents.map { document ->
                    BlockedUser(
                        uid = document.getString("uid") ?: document.id,
                        username = document.getString("username")
                            ?: document.getString("displayName")
                            ?: document.id
                    )
                })
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Could not load blocked users", error)
                onLoaded(emptyList())
            }
    }

    fun getBlockedUserIds(onLoaded: (Set<String>) -> Unit) {
        getBlockedUsers { users -> onLoaded(users.mapTo(mutableSetOf()) { it.uid }) }
    }

    fun isBlocked(otherUid: String, onResult: (Boolean) -> Unit) {
        val uid = currentUid()
        if (uid == null || otherUid.isBlank() || otherUid == uid) {
            onResult(false)
            return
        }

        val ownBlock = db.collection("users").document(uid)
            .collection("blockedUsers").document(otherUid)
        ownBlock.get()
            .addOnSuccessListener { result -> onResult(result.exists()) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Could not check blocked-user status", error)
                onResult(true)
            }
    }

    fun blockUser(otherUid: String, username: String, onResult: (Boolean) -> Unit) {
        val uid = currentUid()
        if (uid == null || otherUid.isBlank() || otherUid == uid) {
            onResult(false)
            return
        }

        val data = hashMapOf<String, Any>(
            "uid" to otherUid,
            "username" to username,
            "blockedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("blockedUsers")
            .document(otherUid)
            .set(data)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Could not block user $otherUid", error)
                onResult(false)
            }
    }

    fun unblockUser(otherUid: String, onResult: (Boolean) -> Unit) {
        val uid = currentUid()
        if (uid == null || otherUid.isBlank()) {
            onResult(false)
            return
        }

        db.collection("users").document(uid).collection("blockedUsers")
            .document(otherUid)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Could not unblock user $otherUid", error)
                onResult(false)
            }
    }

    fun showProfileActions(context: Context, uid: String, displayName: String) {
        showBlockDialog(context, uid, displayName)
    }

    fun showPostActions(context: Context, post: Post) {
        showBlockDialog(context, post.authorUid, post.username)
    }

    fun showCommunityPostActions(context: Context, post: CommunityPost) {
        showBlockDialog(context, post.authorUid, post.authorName)
    }

    private fun showBlockDialog(context: Context, uid: String, name: String) {
        if (uid.isBlank() || uid == currentUid()) return

        AlertDialog.Builder(context)
            .setTitle(name)
            .setItems(arrayOf("Block player", "Cancel")) { dialog, which ->
                if (which == 0) {
                    blockUser(uid, name) { success ->
                        val message = if (success) "Player blocked" else "Could not block player"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .show()
    }
}
