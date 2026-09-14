package com.vire.android.android

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth

fun saveUser(user: User, context: Context) {
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putString("uid", user.id)
        putString("username", user.username)
        putString("email", user.email)
        putString("gender", user.gender)
        putString("dob", user.dateOfBirth)
        putStringSet("games", user.favoriteGames.toSet())
        putStringSet("genres", user.favoriteGenres.toSet())
        putString("skillLevel", user.skillLevel)
        putString("localArea", user.localArea)
        putString("gamerBio", user.gamerBio)
        putString("profileUri", user.profileImageUri?.toString())
        putString("coverUri", user.coverImageUri?.toString())
        putBoolean("is13Plus", user.is13Plus)
        apply()
    }
}

fun loadUser(context: Context): User? {
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val username = prefs.getString("username", null) ?: return null
    val uid = prefs.getString("uid", FirebaseAuth.getInstance().currentUser?.uid ?: "") ?: ""
    val email = prefs.getString("email", "") ?: ""
    val gender = prefs.getString("gender", "") ?: ""
    val dob = prefs.getString("dob", "") ?: ""
    val games = prefs.getStringSet("games", emptySet())?.toList() ?: emptyList()
    val genres = prefs.getStringSet("genres", emptySet())?.toList() ?: emptyList()
    val skillLevel = prefs.getString("skillLevel", "") ?: ""
    val localArea = prefs.getString("localArea", "") ?: ""
    val gamerBio = prefs.getString("gamerBio", "") ?: ""
    val profileUri = prefs.getString("profileUri", null)?.let { Uri.parse(it) }
    val coverUri = prefs.getString("coverUri", null)?.let { Uri.parse(it) }
    val is13Plus = prefs.getBoolean("is13Plus", false)
    return User(
        id = uid,
        username = username,
        email = email,
        gender = gender,
        dateOfBirth = dob,
        favoriteGames = games,
        favoriteGenres = genres,
        skillLevel = skillLevel,
        localArea = localArea,
        gamerBio = gamerBio,
        profileImageUri = profileUri,
        coverImageUri = coverUri,
        is13Plus = is13Plus,
    )
}
