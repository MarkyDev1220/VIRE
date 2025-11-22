package com.example.vire.android

object ChallengeManager {
    private val challenges = mutableListOf<ChallengeRequest>()
    private var lastId = 2000L

    init {
        // seed sample challenges
        addChallenge(
            ChallengeRequest(
                id = nextId(),
                title = "Casual Commander match",
                game = "Magic: The Gathering",
                challenger = "alice",
                opponent = "bob",
                description = "Best of 1, casual, no prize. Available evenings.",
                stake = "Bragging rights"
            )
        )
        addChallenge(
            ChallengeRequest(
                id = nextId(),
                title = "Pokémon Standard Practice",
                game = "Pokemon TCG",
                challenger = "charlie",
                opponent = null,
                description = "Open challenge - looking for Standard practice matches.",
                stake = "Practice"
            )
        )
    }

    fun nextId(): Long = ++lastId

    fun addChallenge(challenge: ChallengeRequest) {
        challenges.add(0, challenge) // newest first
    }

    fun getChallenges(): List<ChallengeRequest> = challenges.toList()

    fun deleteChallenge(id: Long) {
        challenges.removeAll { it.id == id }
    }

    fun acceptChallenge(id: Long, accepter: String) {
        val idx = challenges.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val c = challenges[idx]
            challenges[idx] = c.copy(opponent = accepter, status = ChallengeRequest.Status.ACCEPTED)
        }
    }

    /**
     * Update an existing challenge. Matches by id and replaces the entry (preserves position).
     * Returns true if updated, false if not found.
     */
    fun updateChallenge(updated: ChallengeRequest): Boolean {
        val idx = challenges.indexOfFirst { it.id == updated.id }
        return if (idx >= 0) {
            // Keep the ordering: replace existing entry
            challenges[idx] = updated
            true
        } else {
            false
        }
    }

    /**
     * Search and filter helper.
     * Query checks title, description, challenger, opponent.
     */
    fun searchAndFilter(query: String, game: String): List<ChallengeRequest> {
        val q = query.trim().lowercase()
        return challenges.filter { c ->
            val matchesGame = (game == "All") || c.game.equals(game, ignoreCase = true)
            val matchesQuery = q.isEmpty() || c.title.lowercase().contains(q) ||
                    c.description.lowercase().contains(q) ||
                    c.challenger.lowercase().contains(q) ||
                    (c.opponent?.lowercase()?.contains(q) ?: false)
            matchesGame && matchesQuery
        }
    }
}