package com.vire.android.android

import java.io.Serializable

data class Deck(
    val id: String = java.util.UUID.randomUUID().toString(),
    val deckName: String,
    val game: String, // YuGiOh, Pokemon, CFV, Lorcana, BSS, FOW, MTG
    val description: String = "",
    val cards: MutableList<DeckCard> = mutableListOf(),
    val deckAuthor: String,
    val createdAt: Long = System.currentTimeMillis(),
    var likes: Int = 0,
    val isPublic: Boolean = true
) : Serializable

data class DeckCard(
    val cardId: String,
    val cardName: String,
    val cardImage: String? = null,
    val quantity: Int = 1,
    val rarity: String = "",
    val cardSet: String = "",
    val cost: String = "" // For MTG: mana cost, YuGiOh: level, etc.
) : Serializable

data class Card(
    val id: String,
    val name: String,
    val game: String,
    val imageUrl: String,
    val rarity: String,
    val set: String,
    val cardType: String,
    val description: String,
    val cost: String = ""
) : Serializable
