package com.vire.android.android

import java.io.Serializable

data class Deck(
    val id: String = "",
    val userId: String = "",
    val username: String = "Gamer",
    val deckName: String = "",
    val gameSystem: String = "Magic: The Gathering", // "Yu-Gi-Oh!", "Pokémon", "Cardfight Vanguard (CFV)", "Lorcana", "Battle Spirits Saga (BSS)", "Force of Will (FOW)", "Magic: The Gathering"
    val format: String = "Standard",
    val description: String = "",
    val cards: List<DeckCard> = emptyList(),
    val cardCount: Int = 0,
    val coverImageUrl: String = "",
    val discordUrl: String = "",
    val isPublic: Boolean = true,
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) : Serializable


data class DeckCard(
    val cardName: String = "",
    val gameSystem: String = "Magic: The Gathering",
    val cardType: String = "Main",
    val quantity: Int = 1,
    val imageUrl: String = "",
    val manaCostOrLevel: String = ""
) : Serializable

data class DeckComment(
    val id: String = "",
    val deckId: String = "",
    val authorUid: String = "",
    val authorName: String = "Gamer",
    val authorAvatarUrl: String = "",
    val message: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
