package com.vire.android.android

import java.io.Serializable

data class Community(
    val id: String = "",
    val name: String = "",
    val category: String = "TCG", // "TCG", "RPG", "Miniature Wargame", "Board Game", "Other"
    val description: String = "",
    val iconUrl: String = "",
    val bannerUrl: String = "",
    val memberCount: Int = 0,
    val members: List<String> = emptyList(), // UIDs of joined members
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
