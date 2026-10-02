package com.vire.android.android

import java.io.Serializable

data class CollectionItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val itemType: String = "Game", // "Game", "Deck", "Expansion"
    val statusTag: String = "OWNED", // "OWNED", "WANT", "TRADING", "SELLING"
    val category: String = "Board Game", // "Board Game", "TCG", "Card Game", "RPG", "Other"
    val notes: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
