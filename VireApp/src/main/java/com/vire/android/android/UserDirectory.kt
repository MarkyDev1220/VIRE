package com.vire.android.android

/**
 * Searchable fields only. Email, date of birth, gender, and legal names stay in /users.
 */
object UserDirectory {
    const val COLLECTION = "publicProfiles"

    fun fromUser(user: User): Map<String, Any> {
        val username = normalizedUsername(user.username)
        return mapOf(
            "uid" to user.id,
            "username" to username,
            "usernameLowercase" to username.lowercase(),
            "favoriteGames" to user.favoriteGames.take(20),
            "favoriteGenres" to user.favoriteGenres.take(20),
            "skillLevel" to user.skillLevel.take(100),
            "localArea" to user.localArea.take(200),
            "gamerBio" to user.gamerBio.take(4000),
            "aboutMe" to user.aboutMe.take(4000)
        )
    }

    fun fromPrivateData(uid: String, data: Map<String, Any?>): Map<String, Any> {
        val username = normalizedUsername(data["username"] as? String ?: "Gamer")
        val profileImageUrl = (
            data["profileImageUrl"] as? String
                ?: data["profileImage"] as? String
                ?: data["avatarUrl"] as? String
                ?: ""
            ).takeIf { it.isEmpty() || it.startsWith("https://") }.orEmpty().take(2048)
        val coverImageUrl = (
            data["coverImageUrl"] as? String
                ?: data["coverImage"] as? String
                ?: data["coverUrl"] as? String
                ?: ""
            ).takeIf { it.isEmpty() || it.startsWith("https://") }.orEmpty().take(2048)

        return mapOf(
            "uid" to uid,
            "username" to username,
            "usernameLowercase" to username.lowercase(),
            "favoriteGames" to ((data["favoriteGames"] as? List<*>)?.filterIsInstance<String>()?.take(20) ?: emptyList()),
            "favoriteGenres" to ((data["favoriteGenres"] as? List<*>)?.filterIsInstance<String>()?.take(20) ?: emptyList()),
            "skillLevel" to (data["skillLevel"] as? String ?: "").take(100),
            "localArea" to (data["localArea"] as? String ?: "").take(200),
            "gamerBio" to (data["gamerBio"] as? String ?: "").take(4000),
            "aboutMe" to (data["aboutMe"] as? String ?: "").take(4000),
            "profileImageUrl" to profileImageUrl,
            "coverImageUrl" to coverImageUrl
        )
    }

    private fun normalizedUsername(value: String): String {
        val username = value.trim().take(64)
        return if (username.length >= 3) username else "Gamer"
    }
}
