package com.example.vire.android

object FeedManager {

    // Global feed (newest posts first)
    private val globalFeed = mutableListOf<Post>()

    // Profile feed per user (username -> list of posts)
    private val profileFeeds = mutableMapOf<String, MutableList<Post>>()

    /** Add a post for a user and to the global feed */
    fun addPost(post: Post) {
        // Add to global feed
        globalFeed.add(0, post)

        // Add to user's profile feed
        val userFeed = profileFeeds.getOrPut(post.username) { mutableListOf() }
        userFeed.add(0, post)
    }

    /** Get profile feed */
    fun getProfileFeed(username: String): List<Post> = profileFeeds[username]?.toList() ?: emptyList()

    /** Get global feed */
    fun getGlobalFeed(): List<Post> = globalFeed.toList()

    /** Optional helper: create a post from a game result */
    fun recordGameResult(username: String, result: String, opponent: String) {
        val message = when (result) {
            "win" -> "won against $opponent!"
            "loss" -> "lost to $opponent."
            "tie" -> "tied with $opponent."
            else -> ""
        }
        if (message.isNotEmpty()) {
            val post = Post(username, message)
            addPost(post) // adds to global and profile feed
        }
    }
}
