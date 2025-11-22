package com.example.vire.android

data class ChallengeRequest(
    val id: Long,
    val title: String,
    val game: String,
    val challenger: String,
    val opponent: String?, // could be null for open challenge
    val description: String,
    val stake: String,
    var status: Status = Status.OPEN,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class Status { OPEN, ACCEPTED, CANCELLED }

    fun prettyCreatedAt(): String {
        val d = java.util.Date(createdAt)
        return android.text.format.DateFormat.getMediumDateFormat(null).format(d) + " " +
                android.text.format.DateFormat.getTimeFormat(null).format(d)
    }

    // Convenience helpers so callers can directly ask the model to perform common flows.
    // Note: these call the in-memory ChallengeManager and create coupling between model and manager.
    // It's fine for a small prototype — for production prefer calling a repository/ViewModel from the UI layer.


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
            title = newTitle,
            game = newGame,
            opponent = newOpponent,
            description = newDescription,
            stake = newStake
        )
        return ChallengeManager.updateChallenge(updated)
    }
}