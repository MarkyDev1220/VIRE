package com.example.vire.android

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TournamentRequest(
    val id: Long,
    val name: String,
    val game: String,
    val minPlayers: Int,
    val bracketLink: String,
    val prizesDescription: String?,
    val organizer: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun prettyCreatedAt(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
        return formatter.format(Date(createdAt))
    }
}

