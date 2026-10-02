package com.vire.android.android

import java.io.Serializable

data class PostComment(
    val id: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val authorName: String = "Gamer",
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
