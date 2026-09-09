package com.vire.android.android

import android.net.Uri

data class User(
    val id: Long = 0L,

    // ⭐ REQUIRED FIELDS
    val username: String,
    val email: String,

    // ⭐ OPTIONAL PROFILE FIELDS
    val gender: String = "",
    val dateOfBirth: String = "",
    val favoriteGames: List<String> = emptyList(),

    // ⭐ PROFILE IMAGES
    val profileImageUri: Uri? = null,
    val coverImageUri: Uri? = null,

    // ⭐ AGE CHECK
    val is13Plus: Boolean = true,

    // ⭐ FRIEND SYSTEM (future)
    val friends: MutableList<Long> = mutableListOf()
)

