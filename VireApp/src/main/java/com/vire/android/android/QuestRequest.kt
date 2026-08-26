package com.vire.android.android

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuestRequest(
    val id: Long,
    val title: String,
    val type: Type,
    val game: String,
    val description: String,
    val poster: String,
    val details: String?,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class Type { MATCHES, ISO, TEST }


    fun prettyCreatedAt(): String {
        val fmt = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
        return fmt.format(Date(createdAt))
    }


}
