package com.vire.android.android

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object LocationManager {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val localLocations = mutableListOf<GamingLocation>()
    private val localReviews = mutableMapOf<String, MutableList<StoreReview>>()

    fun createLocation(location: GamingLocation, onResult: (Boolean, String?) -> Unit) {
        try {
            val ref = db.collection("gamingLocations").document()
            val finalLocation = location.copy(id = ref.id)
            localLocations.add(finalLocation)

            if (auth.currentUser != null) {
                ref.set(finalLocation)
                    .addOnSuccessListener { onResult(true, finalLocation.id) }
                    .addOnFailureListener { onResult(true, finalLocation.id) }
            } else {
                onResult(true, finalLocation.id)
            }
        } catch (e: Exception) {
            onResult(false, null)
        }
    }

    fun getLocations(onResult: (List<GamingLocation>) -> Unit) {
        try {
            db.collection("gamingLocations")
                .get()
                .addOnSuccessListener { result ->
                    val list = result.toObjects(GamingLocation::class.java)
                    localLocations.clear()
                    localLocations.addAll(list)
                    onResult(list)
                }
                .addOnFailureListener {
                    onResult(localLocations.toList())
                }
        } catch (e: Exception) {
            onResult(localLocations.toList())
        }
    }

    fun claimLocation(locationId: String, ownerUid: String, onResult: (Boolean) -> Unit) {
        try {
            val index = localLocations.indexOfFirst { it.id == locationId }
            if (index >= 0) {
                val updated = localLocations[index].copy(
                    claimedByUid = ownerUid,
                    isClaimed = true
                )
                localLocations[index] = updated
            }

            if (auth.currentUser != null && locationId.isNotEmpty()) {
                db.collection("gamingLocations").document(locationId)
                    .update(
                        mapOf(
                            "claimedByUid" to ownerUid,
                            "isClaimed" to true
                        )
                    )
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(true) }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun addReview(review: StoreReview, onResult: (Boolean) -> Unit) {
        try {
            val ref = db.collection("storeReviews").document()
            val finalReview = review.copy(id = ref.id)

            val reviewsList = localReviews.getOrPut(review.locationId) { mutableListOf() }
            reviewsList.add(0, finalReview)

            // Update local location rating
            val index = localLocations.indexOfFirst { it.id == review.locationId }
            if (index >= 0) {
                val loc = localLocations[index]
                val newSum = loc.ratingSum + review.rating
                val newCount = loc.reviewCount + 1
                localLocations[index] = loc.copy(ratingSum = newSum, reviewCount = newCount)
            }

            if (auth.currentUser != null) {
                ref.set(finalReview).addOnSuccessListener {
                    // Update location document stats in Firestore
                    if (index >= 0) {
                        val loc = localLocations[index]
                        db.collection("gamingLocations").document(review.locationId)
                            .update(
                                mapOf(
                                    "ratingSum" to loc.ratingSum,
                                    "reviewCount" to loc.reviewCount
                                )
                            )
                    }
                    onResult(true)
                }.addOnFailureListener {
                    onResult(true)
                }
            } else {
                onResult(true)
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun getReviewsForLocation(locationId: String, onResult: (List<StoreReview>) -> Unit) {
        try {
            db.collection("storeReviews")
                .whereEqualTo("locationId", locationId)
                .get()
                .addOnSuccessListener { result ->
                    val list = result.toObjects(StoreReview::class.java)
                    localReviews[locationId] = list.toMutableList()
                    onResult(list)
                }
                .addOnFailureListener {
                    onResult(localReviews[locationId] ?: emptyList())
                }
        } catch (e: Exception) {
            onResult(localReviews[locationId] ?: emptyList())
        }
    }

    fun filterLocations(
        locations: List<GamingLocation>,
        query: String,
        typeFilter: String
    ): List<GamingLocation> {
        val q = query.trim().lowercase()
        return locations.filter { loc ->
            val matchesQuery = q.isEmpty() ||
                    loc.name.lowercase().contains(q) ||
                    loc.city.lowercase().contains(q) ||
                    loc.state.lowercase().contains(q) ||
                    loc.description.lowercase().contains(q) ||
                    loc.supportedGames.any { it.lowercase().contains(q) }

            val matchesType = typeFilter.equals("All", ignoreCase = true) ||
                    loc.type.equals(typeFilter, ignoreCase = true)

            matchesQuery && matchesType
        }
    }
}
