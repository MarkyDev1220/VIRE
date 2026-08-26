package com.vire.android.android

data class GameLeaderboard(
    val gameName: String,
    val players: MutableList<Player> = mutableListOf()
)