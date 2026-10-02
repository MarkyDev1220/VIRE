package com.vire.android.android

import java.io.Serializable

data class Tournament(
    val id: String = "",
    val name: String = "",
    val gameSystem: String = "Magic: The Gathering",
    val format: String = "Swiss", // "Swiss", "Single Elimination", "Double Elimination", "Casual"
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val entryFee: String = "Free",
    val maxParticipants: Int = 16,
    val rules: String = "",
    val prizesDescription: String = "",
    val organizer: String = "Organizer",
    val hostUid: String = "",
    val status: String = "Upcoming", // "Upcoming", "In Progress", "Completed"
    val discordUrl: String = "",
    val externalBracketUrl: String = "",
    val participants: List<String> = emptyList(), // UIDs of registered players
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
