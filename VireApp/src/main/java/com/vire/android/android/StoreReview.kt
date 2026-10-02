package com.vire.android.android

import java.io.Serializable

data class StoreReview(
    val id: String = "",
    val locationId: String = "",
    val authorUid: String = "",
    val authorUsername: String = "Gamer",
    val rating: Float = 5.0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
