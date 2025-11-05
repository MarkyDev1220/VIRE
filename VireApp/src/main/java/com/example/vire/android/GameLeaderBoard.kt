package com.example.vire.android

data class GameLeaderboard(
    val gameName: String,
    val players: MutableList<Player> = mutableListOf()
)