package com.vire.android.android

object FeedManager {

    private val globalFeed = mutableListOf<Post>()
    private val profileFeeds = mutableMapOf<String, MutableList<Post>>()

    /**
     * Add a new post to the global feed and the user's profile feed.
     * Username is now ALWAYS the real username passed from HomeActivity → NewPostActivity.
     */
    fun addPost(
        username: String,
        content: String,
        imageUri: String? = null
    ) {
        val post = Post(
            username = username,
            content = content,
            imageUri = imageUri
        )

        // Add to global feed (newest at top)
        globalFeed.add(0, post)

        // Add to user's profile feed
        val userFeed = profileFeeds.getOrPut(username) { mutableListOf() }
        userFeed.add(0, post)
    }

    /**
     * Get posts for a specific user's profile.
     */
    fun getProfileFeed(username: String): List<Post> =
        profileFeeds[username]?.toList() ?: emptyList()

    /**
     * Get the global feed.
     */
    fun getGlobalFeed(): List<Post> = globalFeed.toList()

    /**
     * Add a comment to a post.
     * Username is ALWAYS the real username passed from UI.
     */
    fun addComment(postId: String, username: String, text: String) {
        val comment = Comment(username, text)
        val post = globalFeed.find { it.id == postId }
        post?.comments?.add(comment)
    }

    /**
     * Like a post.
     */
    fun likePost(postId: String) {
        val post = globalFeed.find { it.id == postId }
        if (post != null) {
            post.likes++
        }
    }
}
