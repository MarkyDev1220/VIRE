package com.example.vire.android

object LeaderboardManager {

    private val leaderboards: MutableList<GameLeaderboard> = mutableListOf()

    init {
        // Existing games
        leaderboards.add(
            GameLeaderboard(
                "Magic: The Gathering",
                mutableListOf(
                    Player("Player1", 15),
                    Player("Player2", 20),
                    Player("Player3", 10)
                )
            )
        )
        leaderboards.add(
            GameLeaderboard(
                "Pokemon TCG",
                mutableListOf(
                    Player("PlayerA", 12),
                    Player("PlayerB", 18),
                    Player("PlayerC", 9)
                )
            )
        )
        leaderboards.add(
            GameLeaderboard(
                "Yu-Gi-Oh!",
                mutableListOf(
                    Player("Duelist1", 25),
                    Player("Duelist2", 15),
                    Player("Duelist3", 5)
                )
            )
        )

        // New games
        leaderboards.add(
            GameLeaderboard(
                "Battle Spirits Saga (BSS)",
                mutableListOf(
                    Player("BSSPlayer1", 10),
                    Player("BSSPlayer2", 20),
                    Player("BSSPlayer3", 15)
                )
            )
        )
        leaderboards.add(
            GameLeaderboard(
                "Cardfight Vanguard (CFV)",
                mutableListOf(
                    Player("CFVPlayer1", 18),
                    Player("CFVPlayer2", 12),
                    Player("CFVPlayer3", 25)
                )
            )
        )
        leaderboards.add(
            GameLeaderboard(
                "Lorcana",
                mutableListOf(
                    Player("LorcanaPlayer1", 14),
                    Player("LorcanaPlayer2", 22),
                    Player("LorcanaPlayer3", 9)
                )
            )
        )
        leaderboards.add(
            GameLeaderboard(
                "Force of Will (FOW)",
                mutableListOf(
                    Player("FOWPlayer1", 30),
                    Player("FOWPlayer2", 18),
                    Player("FOWPlayer3", 21)
                )
            )
        )
    }

    fun getLeaderboard(gameName: String): List<Player> {
        return leaderboards.find { it.gameName == gameName }?.players
            ?.sortedByDescending { it.points } ?: emptyList()
    }

    fun addPlayer(gameName: String, player: Player) {
        val game = leaderboards.find { it.gameName == gameName }
        game?.players?.add(player)
    }

    fun updatePoints(gameName: String, username: String, points: Int) {
        val player = leaderboards.find { it.gameName == gameName }?.players?.find { it.username == username }
        player?.points = points
    }

    fun getGames(): List<String> = leaderboards.map { it.gameName }
}
