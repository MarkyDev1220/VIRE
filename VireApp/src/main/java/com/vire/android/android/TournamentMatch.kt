package com.vire.android.android

import java.io.Serializable

data class TournamentMatch(
    val id: String = "",
    val tournamentId: String = "",
    val roundNumber: Int = 1,
    val matchNumber: Int = 1,
    val player1Uid: String = "",
    val player1Name: String = "Player 1",
    val player2Uid: String = "",
    val player2Name: String = "Player 2",
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val winnerUid: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
