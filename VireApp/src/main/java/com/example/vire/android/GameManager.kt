// File: GameManager.kt
package com.example.vire.android

object GameManager {

    fun recordGameResult(username: String, result: String, opponent: String) {
        // TODO: save the game result in your database or in-memory list

        // Automatically create a feed post
        val message = when (result) {
            "win" -> "won against $opponent!"
            "loss" -> "lost to $opponent."
            "tie" -> "tied with $opponent."
            else -> ""
        }

        if (message.isNotEmpty()) {
            val post = Post(username, message)
            FeedManager.addPost(post) // FeedManager stores all posts
        }
    }
}
