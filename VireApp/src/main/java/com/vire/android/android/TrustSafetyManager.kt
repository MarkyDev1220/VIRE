package com.vire.android.android

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class BlockedUser(
    val uid: String = "",
    val username: String = ""
)

object TrustSafetyManager {
    private val localBlockedUsers = mutableListOf<BlockedUser>()
    private val localBlockedIds = mutableSetOf<String>()

    private fun getCurrentUid(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }

    private fun getDb(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    fun getBlockedUsers(callback: (List<BlockedUser>) -> Unit) {
        val uid = getCurrentUid()
        val db = getDb()
        if (uid == null || db == null) {
            callback(localBlockedUsers.toList())
            return
        }

        db.collection("users").document(uid).collection("blockedUsers")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    val targetUid = doc.getString("uid") ?: doc.id
                    val username = doc.getString("username") ?: "User"
                    BlockedUser(targetUid, username)
                }
                localBlockedUsers.clear()
                localBlockedUsers.addAll(list)
                localBlockedIds.clear()
                localBlockedIds.addAll(list.map { it.uid })
                callback(list)
            }
            .addOnFailureListener {
                callback(localBlockedUsers.toList())
            }
    }

    fun getBlockedUserIds(callback: (Set<String>) -> Unit) {
        getBlockedUsers { users ->
            callback(users.map { it.uid }.toSet())
        }
    }

    fun isBlocked(targetUid: String, callback: (Boolean) -> Unit) {
        if (localBlockedIds.contains(targetUid)) {
            callback(true)
            return
        }
        getBlockedUserIds { blocked ->
            callback(blocked.contains(targetUid))
        }
    }

    fun blockUser(targetUid: String, targetUsername: String, callback: ((Boolean) -> Unit)? = null) {
        val uid = getCurrentUid()
        val db = getDb()
        localBlockedIds.add(targetUid)
        localBlockedUsers.removeAll { it.uid == targetUid }
        localBlockedUsers.add(BlockedUser(targetUid, targetUsername))

        if (uid == null || db == null) {
            callback?.invoke(true)
            return
        }

        val data = hashMapOf(
            "uid" to targetUid,
            "username" to targetUsername,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(uid).collection("blockedUsers").document(targetUid)
            .set(data)
            .addOnSuccessListener { callback?.invoke(true) }
            .addOnFailureListener { callback?.invoke(false) }
    }

    fun unblockUser(targetUid: String, callback: ((Boolean) -> Unit)? = null) {
        val uid = getCurrentUid()
        val db = getDb()
        localBlockedIds.remove(targetUid)
        localBlockedUsers.removeAll { it.uid == targetUid }

        if (uid == null || db == null) {
            callback?.invoke(true)
            return
        }

        db.collection("users").document(uid).collection("blockedUsers").document(targetUid)
            .delete()
            .addOnSuccessListener { callback?.invoke(true) }
            .addOnFailureListener { callback?.invoke(false) }
    }

    fun showPostActions(context: Context, post: Post) {
        val options = arrayOf("Report Post", "Block ${post.username}")
        AlertDialog.Builder(context)
            .setTitle("Post Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> reportContent(context, "post", post.id, post.authorUid)
                    1 -> confirmBlock(context, post.authorUid, post.username)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showCommunityPostActions(context: Context, post: CommunityPost) {
        val options = arrayOf("Report Post", "Block ${post.authorName}")
        AlertDialog.Builder(context)
            .setTitle("Post Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> reportContent(context, "community_post", post.id, post.authorUid)
                    1 -> confirmBlock(context, post.authorUid, post.authorName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showProfileActions(context: Context, targetUid: String, displayName: String) {
        val options = arrayOf("Report Player", "Block $displayName")
        AlertDialog.Builder(context)
            .setTitle("Player Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> reportContent(context, "user", targetUid, targetUid)
                    1 -> confirmBlock(context, targetUid, displayName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmBlock(context: Context, targetUid: String, username: String) {
        AlertDialog.Builder(context)
            .setTitle("Block $username?")
            .setMessage("You will no longer see posts, messages, or activity from this user.")
            .setPositiveButton("Block") { _, _ ->
                blockUser(targetUid, username) { success ->
                    if (success) {
                        Toast.makeText(context, "$username has been blocked.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to block user.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reportContent(context: Context, type: String, contentId: String, authorUid: String) {
        val reasons = arrayOf("Inappropriate content", "Harassment / Hate speech", "Spam / Scam", "Other")
        AlertDialog.Builder(context)
            .setTitle("Report")
            .setItems(reasons) { _, which ->
                val reason = reasons[which]
                val uid = getCurrentUid()
                val db = getDb()
                if (uid != null && db != null) {
                    val reportData = hashMapOf(
                        "reporterUid" to uid,
                        "contentType" to type,
                        "contentId" to contentId,
                        "authorUid" to authorUid,
                        "reason" to reason,
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("reports").add(reportData)
                }
                Toast.makeText(context, "Thank you. Your report has been submitted for review.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
