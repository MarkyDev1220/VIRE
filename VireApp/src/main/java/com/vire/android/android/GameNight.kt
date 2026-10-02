package com.vire.android.android

import java.io.Serializable

data class GameNight(
    val id: String = "",
    val hostUid: String = "",
    val hostUsername: String = "",
    val gameTitle: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val description: String = "",
    val tier: String = "Casual Tier", // "Casual Tier", "Competitive Tier", "Pro Tier"
    val maxParticipants: Int = 4,
    val participants: List<String> = emptyList(),
    val maxSpectators: Int = 10,
    val spectators: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
