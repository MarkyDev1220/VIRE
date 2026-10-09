package com.vire.android.android

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FeedManager {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val localFeed = mutableListOf<Post>()
    private val localComments = mutableMapOf<String, MutableList<Comment>>()

    fun addPost(
        username: String,
        content: String,
        imageUri: String? = null,
        postType: String = "General",
        onResult: ((Boolean) -> Unit)? = null
    ) {
        try {
            val ref = db.collection("globalPosts").document()
            val uid = auth.currentUser?.uid ?: "demo_user"

            val post = Post(
                id = ref.id,
                authorUid = uid,
                username = username,
                content = content,
                imageUri = imageUri,
                postType = postType,
                timestamp = System.currentTimeMillis()
            )

            localFeed.add(0, post)

            if (auth.currentUser != null) {
                ref.set(post)
                    .addOnSuccessListener { onResult?.invoke(true) }
                    .addOnFailureListener { onResult?.invoke(true) }
            } else {
                onResult?.invoke(true)
            }
        } catch (e: Exception) {
            onResult?.invoke(false)
        }
    }

    fun fetchGlobalFeed(onResult: (List<Post>) -> Unit) {
        try {
            db.collection("globalPosts")
                .get()
                .addOnSuccessListener { result ->
                    val list = result.toObjects(Post::class.java)
                    localFeed.clear()
                    localFeed.addAll(list)
                    onResult(list)
                }
                .addOnFailureListener {
                    onResult(localFeed.toList())
                }
        } catch (e: Exception) {
            onResult(localFeed.toList())
        }
    }

    fun getGlobalFeed(): List<Post> = localFeed.toList()

    fun getProfileFeed(username: String): List<Post> =
        localFeed.filter { it.username.equals(username, ignoreCase = true) }

    fun toggleLikePost(postId: String, userUid: String, onResult: ((Boolean) -> Unit)? = null) {
        try {
            val idx = localFeed.indexOfFirst { it.id == postId }
            if (idx >= 0) {
                val p = localFeed[idx]
                val isLiked = p.likedBy.contains(userUid)
                val newLikedBy = if (isLiked) p.likedBy - userUid else p.likedBy + userUid
                val newLikes = newLikedBy.size
                localFeed[idx] = p.copy(likedBy = newLikedBy, likes = newLikes)

                if (auth.currentUser != null && postId.isNotEmpty()) {
                    val ref = db.collection("globalPosts").document(postId)
                    if (isLiked) {
                        ref.update("likedBy", FieldValue.arrayRemove(userUid), "likes", FieldValue.increment(-1))
                    } else {
                        ref.update("likedBy", FieldValue.arrayUnion(userUid), "likes", FieldValue.increment(1))
                    }
                }
            }
            onResult?.invoke(true)
        } catch (e: Exception) {
            onResult?.invoke(false)
        }
    }

    fun addComment(postId: String, username: String, text: String, onResult: ((Boolean) -> Unit)? = null) {
        try {
            val ref = db.collection("postComments").document()
            val uid = auth.currentUser?.uid ?: "demo_user"
            val comment = Comment(
                id = ref.id,
                postId = postId,
                authorUid = uid,
                username = username,
                text = text
            )

            val list = localComments.getOrPut(postId) { mutableListOf() }
            list.add(comment)

            val idx = localFeed.indexOfFirst { it.id == postId }
            if (idx >= 0) {
                val p = localFeed[idx]
                localFeed[idx] = p.copy(commentsCount = list.size)
            }

            if (auth.currentUser != null) {
                val commentMap = hashMapOf(
                    "id" to ref.id,
                    "postId" to postId,
                    "authorUid" to uid,
                    "username" to username,
                    "authorName" to username,
                    "text" to text,
                    "comment" to text,
                    "timestamp" to System.currentTimeMillis()
                )
                ref.set(commentMap)
                if (postId.isNotEmpty()) {
                    db.collection("globalPosts").document(postId)
                        .update("commentsCount", FieldValue.increment(1))
                }
            }
            onResult?.invoke(true)
        } catch (e: Exception) {
            onResult?.invoke(false)
        }
    }

    fun deleteComment(postId: String, commentId: String, onResult: ((Boolean) -> Unit)? = null) {
        try {
            // Remove from local comments list
            val list = localComments[postId]
            list?.removeAll { it.id == commentId }
            val newCount = list?.size ?: 0

            // Decrement comments count locally
            val idx = localFeed.indexOfFirst { it.id == postId }
            if (idx >= 0) {
                val p = localFeed[idx]
                localFeed[idx] = p.copy(commentsCount = newCount)
            }

            // Remove from Firestore
            if (auth.currentUser != null && commentId.isNotEmpty()) {
                db.collection("postComments").document(commentId).delete()
                if (postId.isNotEmpty()) {
                    db.collection("globalPosts").document(postId)
                        .update("commentsCount", FieldValue.increment(-1))
                }
            }
            onResult?.invoke(true)
        } catch (e: Exception) {
            onResult?.invoke(false)
        }
    }

    fun getComments(postId: String, onResult: (List<Comment>) -> Unit) {
        if (postId.isEmpty()) {
            onResult(emptyList())
            return
        }
        val cached = localComments[postId]
        if (!cached.isNullOrEmpty()) {
            onResult(cached)
            return
        }
        try {
            db.collection("postComments")
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener { result ->
                    val list = mutableListOf<Comment>()
                    for (doc in result.documents) {
                        val c = doc.toObject(Comment::class.java)
                        val u = doc.getString("username") ?: doc.getString("authorName") ?: c?.username ?: "Gamer"
                        val t = doc.getString("text") ?: doc.getString("comment") ?: c?.text ?: ""
                        val aUid = doc.getString("authorUid") ?: c?.authorUid ?: ""
                        val commentId = doc.id
                        if (c != null) {
                            list.add(c.copy(id = commentId, authorUid = aUid, username = u, text = t))
                        } else if (t.isNotEmpty()) {
                            list.add(Comment(id = commentId, postId = postId, authorUid = aUid, username = u, text = t))
                        }
                    }
                    localComments[postId] = list
                    onResult(list)
                }
                .addOnFailureListener {
                    onResult(localComments[postId] ?: emptyList())
                }
        } catch (e: Exception) {
            onResult(localComments[postId] ?: emptyList())
        }
    }
}
