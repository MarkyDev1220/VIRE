package com.vire.android.android

import android.content.Context
import android.text.format.DateFormat
import java.util.*

data class ChallengeRequest(
    val id: Long,
    val title: String = "Untitled Challenge",
    val game: String = "Unknown Game",
    val challenger: String = "Unknown",
    val opponent: String? = null, // null means open challenge
    val description: String = "",
    val stake: String = "",
    var status: Status = Status.OPEN,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class Status { OPEN, ACCEPTED, CANCELLED }

    /** Pretty formatted date for UI */
    fun prettyCreatedAt(context: Context): String {
        val d = Date(createdAt)
        return DateFormat.getMediumDateFormat(context).format(d) + " " +
                DateFormat.getTimeFormat(context).format(d)
    }


    /** Convenience methods for ChallengeManager flows */
    fun delete() {
        ChallengeManager.deleteChallenge(id)
    }

    fun accept(asUser: String) {
        ChallengeManager.acceptChallenge(id, asUser)
    }

    fun update(
        newTitle: String,
        newGame: String,
        newOpponent: String?,
        newDescription: String,
        newStake: String
    ): Boolean {
        val updated = this.copy(
            title = newTitle.ifEmpty { this.title },
            game = newGame.ifEmpty { this.game },
            opponent = newOpponent ?: this.opponent,
            description = newDescription.ifEmpty { this.description },
            stake = newStake.ifEmpty { this.stake }
        )
        return ChallengeManager.updateChallenge(updated)
    }

    /** Returns a safe truncated description for UI */
    fun safeDescription(maxLength: Int = 120): String {
        val text = description.ifEmpty { "No description provided" }
        return if (text.length > maxLength) text.take(maxLength - 3) + "..." else text
    }
}
