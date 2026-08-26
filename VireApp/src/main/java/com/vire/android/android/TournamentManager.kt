package com.vire.android.android

object TournamentManager {
    private val tournaments = mutableListOf<TournamentRequest>()
    private var lastId = 4000L

    init {
        // example seed
        addTournament(
            TournamentRequest(
                id = nextId(),
                name = "Local MTG Weekly Cup",
                game = "Magic: The Gathering",
                minPlayers = 16,
                bracketLink = "https://challonge.com/example",
                prizesDescription = "Prize: 1 deck",
                organizer = "alice"
            )
        )
    }

    fun nextId(): Long = ++lastId

    fun addTournament(t: TournamentRequest) {
        tournaments.add(0, t)
    }

    fun getTournaments(): List<TournamentRequest> = tournaments.toList()

    fun deleteTournament(id: Long) {
        tournaments.removeAll { it.id == id }
    }

    fun updateTournament(updated: TournamentRequest): Boolean {
        val idx = tournaments.indexOfFirst { it.id == updated.id }
        return if (idx >= 0) {
            tournaments[idx] = updated
            true
        } else {
            false
        }
    }

    /**
     * Simple search filter (by name or game).
     */
    fun search(query: String): List<TournamentRequest> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return getTournaments()
        return tournaments.filter {
            it.name.lowercase().contains(q) || it.game.lowercase().contains(q)
        }
    }
}