package com.vire.android.android

import java.io.Serializable

data class Deck(
    val description: String = "",
) : Serializable

data class DeckCard(
    val quantity: Int = 1,
) : Serializable

) : Serializable
