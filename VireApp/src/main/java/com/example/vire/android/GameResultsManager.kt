package com.example.vire.android

object GameResultManager {

    fun recordGameResult(username: String, result: String, opponent: String) {

        val message = when (result.lowercase()) {
            "win" -> "won against $opponent!"
            "loss" -> "lost to $opponent."
            "tie" -> "tied with $opponent."
            else -> return
        }

        // NEW: use updated FeedManager API
        FeedManager.addPost(
            username = username,
            content = message,
            imageUri = null
        )
    }
}
