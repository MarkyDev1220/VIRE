package com.example.vire.android

object FeedManager {

    private val globalFeed = mutableListOf<Post>()
    private val profileFeeds = mutableMapOf<String, MutableList<Post>>()

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

        globalFeed.add(0, post)

        val userFeed = profileFeeds.getOrPut(username) { mutableListOf() }
        userFeed.add(0, post)
    }

    fun getProfileFeed(username: String): List<Post> =
        profileFeeds[username]?.toList() ?: emptyList()

    fun getGlobalFeed(): List<Post> = globalFeed.toList()

    fun addComment(postId: String, username: String, text: String) {
        val comment = Comment(username, text)
        val post = globalFeed.find { it.id == postId }
        post?.comments?.add(comment)
    }

    fun likePost(postId: String) {
        val post = globalFeed.find { it.id == postId }
        if (post != null) {
            post.likes++
        }
    }

}

