package com.example.vire.android

import android.content.Context
import android.net.Uri

fun saveUser(user: User, context: Context) {
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putString("username", user.username)
        putString("email", user.email)
        putString("gender", user.gender)
        putString("dob", user.dateOfBirth)
        putStringSet("games", user.favoriteGames.toSet())
        putString("profileUri", user.profileImageUri?.toString())
        putBoolean("is13Plus", user.is13Plus)
        apply()
    }
}

fun loadUser(context: Context): User? {
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val username = prefs.getString("username", null) ?: return null
    val email = prefs.getString("email", "") ?: ""
    val gender = prefs.getString("gender", "") ?: ""
    val dob = prefs.getString("dob", "") ?: ""
    val games = prefs.getStringSet("games", emptySet())?.toList() ?: emptyList()
    val profileUri = prefs.getString("profileUri", null)?.let { Uri.parse(it) }
    val is13Plus = prefs.getBoolean("is13Plus", false)
    return User(
        id = 1,
        username = username,
        email = email,
        gender = gender,
        dateOfBirth = dob,
        favoriteGames = games,
        profileImageUri = profileUri,
        is13Plus = is13Plus
    )
}
