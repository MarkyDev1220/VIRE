package com.example.vire.android


object GameResultManager {

    fun recordGameResult(username: String, result: String, opponent: String) {
        // Create message
        val message = when (result.lowercase()) {
            "win" -> "won against $opponent!"
            "loss" -> "lost to $opponent."
            "tie" -> "tied with $opponent."
            else -> return
        }

        // Create Post object
        val post = Post(username, message)

        // Add post to FeedManager (updates global feed + profile feed)
        FeedManager.addPost(post)
    }
}
