package com.vire.android.android

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

object GameNightManager {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun createGameNight(gameNight: GameNight, onResult: (Boolean) -> Unit) {
        try {
            val ref = db.collection("gameNights").document()
            val finalGameNight = gameNight.copy(
                id = ref.id,
                hostUid = auth.currentUser?.uid ?: "",
                participants = listOf(auth.currentUser?.uid ?: "")
            )
            ref.set(finalGameNight)
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { onResult(false) }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun getGameNights(onResult: (List<GameNight>) -> Unit) {
        try {
            db.collection("gameNights")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener { result ->
                    val list = result.toObjects(GameNight::class.java)
                    onResult(list)
                }
                .addOnFailureListener { onResult(emptyList()) }
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }

    fun joinGameNight(gameNightId: String, onResult: (Boolean) -> Unit) {
        try {
            val uid = auth.currentUser?.uid ?: return onResult(false)
            db.collection("gameNights").document(gameNightId)
                .update("participants", FieldValue.arrayUnion(uid))
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { onResult(false) }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun leaveGameNight(gameNightId: String, onResult: (Boolean) -> Unit) {
        try {
            val uid = auth.currentUser?.uid ?: return onResult(false)
            db.collection("gameNights").document(gameNightId)
                .update("participants", FieldValue.arrayRemove(uid))
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { onResult(false) }
        } catch (e: Exception) {
            onResult(false)
        }
    }
    
    fun deleteGameNight(gameNightId: String, onResult: (Boolean) -> Unit) {
        try {
            db.collection("gameNights").document(gameNightId)
                .delete()
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { onResult(false) }
        } catch (e: Exception) {
            onResult(false)
        }
    }
}
