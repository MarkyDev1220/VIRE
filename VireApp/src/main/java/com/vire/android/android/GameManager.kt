package com.vire.android.android

object GameManager {

    fun recordGameResult(username: String, result: String, opponent: String) {

        val message = when (result) {
            "win" -> "won against $opponent!"
            "loss" -> "lost to $opponent."
            "tie" -> "tied with $opponent."
            else -> ""
        }

        if (message.isNotEmpty()) {
            // NEW: use updated FeedManager API
            FeedManager.addPost(
                username = username,
                content = message,
                imageUri = null
            )
        }
    }
}
