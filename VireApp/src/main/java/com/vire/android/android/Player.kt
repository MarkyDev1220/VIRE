package com.vire.android.android

data class Player(
    val username: String,
    var points: Int,
    val game: String,   // The game this player belongs to
    val avatarUrl: String? = null,
    var wins: Int = 0,
    var losses: Int = 0,
    var ties: Int = 0
)

