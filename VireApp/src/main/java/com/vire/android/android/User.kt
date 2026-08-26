package com.vire.android.android

import android.net.Uri

data class User(
    val id: Long = 0L,
    val username: String,
    val email: String,
    val gender: String,
    val dateOfBirth: String,
    val favoriteGames: List<String>,
    val profileImageUri: Uri? = null,
    val is13Plus: Boolean = true,
    val friends: MutableList<Long> = mutableListOf()
)

