package com.vire.android.android

import java.io.Serializable

data class GamingLocation(
    val id: String = "",
    val name: String = "",
    val type: String = "Local Game Store", // "Local Game Store", "Card Shop", "Board Game Café", "Gaming Group", "Convention"
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val phoneNumber: String = "",
    val websiteUrl: String = "",
    val supportedGames: List<String> = emptyList(),
    val claimedByUid: String = "",
    val isClaimed: Boolean = false,
    val ratingSum: Float = 0f,
    val reviewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {

    fun averageRating(): Float {
        return if (reviewCount > 0) ratingSum / reviewCount else 0f
    }
}
