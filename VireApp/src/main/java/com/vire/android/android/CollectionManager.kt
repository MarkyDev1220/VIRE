package com.vire.android.android

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object CollectionManager {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val localCollection = mutableListOf<CollectionItem>()

    fun addCollectionItem(item: CollectionItem, onResult: (Boolean, String?) -> Unit) {
        try {
            val uid = auth.currentUser?.uid ?: "demo_user"
            val ref = db.collection("collections").document()
            val newItem = item.copy(
                id = ref.id,
                userId = uid
            )
            localCollection.add(newItem)
            if (auth.currentUser != null) {
                ref.set(newItem)
                    .addOnSuccessListener { onResult(true, newItem.id) }
                    .addOnFailureListener {
                        // Safe fallback - saved locally
                        onResult(true, newItem.id)
                    }
            } else {
                onResult(true, newItem.id)
            }
        } catch (e: Exception) {
            onResult(false, null)
        }
    }

    fun updateCollectionItem(item: CollectionItem, onResult: (Boolean) -> Unit) {
        try {
            val index = localCollection.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                localCollection[index] = item
            } else {
                localCollection.add(item)
            }
            if (auth.currentUser != null && item.id.isNotEmpty()) {
                db.collection("collections").document(item.id)
                    .set(item)
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun deleteCollectionItem(itemId: String, onResult: (Boolean) -> Unit) {
        try {
            localCollection.removeAll { it.id == itemId }
            if (auth.currentUser != null && itemId.isNotEmpty()) {
                db.collection("collections").document(itemId)
                    .delete()
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun getUserCollection(targetUserId: String? = null, onResult: (List<CollectionItem>) -> Unit) {
        try {
            val uid = targetUserId ?: auth.currentUser?.uid
            if (uid.isNullOrEmpty() || auth.currentUser == null) {
                onResult(localCollection.toList())
                return
            }

            db.collection("collections")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { result ->
                    val items = result.toObjects(CollectionItem::class.java)
                    localCollection.clear()
                    localCollection.addAll(items)
                    onResult(items)
                }
                .addOnFailureListener {
                    onResult(localCollection.toList())
                }
        } catch (e: Exception) {
            onResult(localCollection.toList())
        }
    }

    fun filterCollection(
        items: List<CollectionItem>,
        query: String,
        itemType: String,
        statusTag: String
    ): List<CollectionItem> {
        val q = query.trim().lowercase()
        return items.filter { item ->
            val matchesQuery = q.isEmpty() ||
                    item.title.lowercase().contains(q) ||
                    item.category.lowercase().contains(q) ||
                    item.notes.lowercase().contains(q)
            val matchesType = itemType.equals("All", ignoreCase = true) || item.itemType.equals(itemType, ignoreCase = true)
            val matchesStatus = statusTag.equals("All", ignoreCase = true) || item.statusTag.equals(statusTag, ignoreCase = true)

            matchesQuery && matchesType && matchesStatus
        }
    }
}
