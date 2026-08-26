package com.vire.android.android

object LeaderboardManager {

    private val leaderboards: MutableList<GameLeaderboard> = mutableListOf()

    init {
        // Magic: The Gathering
        leaderboards.add(
            GameLeaderboard(
                "Magic: The Gathering",
                mutableListOf(
                    Player("Player1", 15, "Magic: The Gathering"),
                    Player("Player2", 20, "Magic: The Gathering"),
                    Player("Player3", 10, "Magic: The Gathering")
                )
            )
        )

        // Pokemon TCG
        leaderboards.add(
            GameLeaderboard(
                "Pokemon TCG",
                mutableListOf(
                    Player("PlayerA", 12, "Pokemon TCG"),
                    Player("PlayerB", 18, "Pokemon TCG"),
                    Player("PlayerC", 9, "Pokemon TCG")
                )
            )
        )

        // Yu-Gi-Oh!
        leaderboards.add(
            GameLeaderboard(
                "Yu-Gi-Oh!",
                mutableListOf(
                    Player("Duelist1", 25, "Yu-Gi-Oh!"),
                    Player("Duelist2", 15, "Yu-Gi-Oh!"),
                    Player("Duelist3", 5, "Yu-Gi-Oh!")
                )
            )
        )

        // Battle Spirits Saga (BSS)
        leaderboards.add(
            GameLeaderboard(
                "Battle Spirits Saga (BSS)",
                mutableListOf(
                    Player("BSSPlayer1", 10, "Battle Spirits Saga (BSS)"),
                    Player("BSSPlayer2", 20, "Battle Spirits Saga (BSS)"),
                    Player("BSSPlayer3", 15, "Battle Spirits Saga (BSS)")
                )
            )
        )

        // Cardfight Vanguard (CFV)
        leaderboards.add(
            GameLeaderboard(
                "Cardfight Vanguard (CFV)",
                mutableListOf(
                    Player("CFVPlayer1", 18, "Cardfight Vanguard (CFV)"),
                    Player("CFVPlayer2", 12, "Cardfight Vanguard (CFV)"),
                    Player("CFVPlayer3", 25, "Cardfight Vanguard (CFV)")
                )
            )
        )

        // Lorcana
        leaderboards.add(
            GameLeaderboard(
                "Lorcana",
                mutableListOf(
                    Player("LorcanaPlayer1", 14, "Lorcana"),
                    Player("LorcanaPlayer2", 22, "Lorcana"),
                    Player("LorcanaPlayer3", 9, "Lorcana")
                )
            )
        )

        // Force of Will (FOW)
        leaderboards.add(
            GameLeaderboard(
                "Force of Will (FOW)",
                mutableListOf(
                    Player("FOWPlayer1", 30, "Force of Will (FOW)"),
                    Player("FOWPlayer2", 18, "Force of Will (FOW)"),
                    Player("FOWPlayer3", 21, "Force of Will (FOW)")
                )
            )
        )
    }

    fun getLeaderboard(gameName: String): List<Player> {
        return leaderboards.find { it.gameName == gameName }?.players
            ?.sortedByDescending { it.points } ?: emptyList()
    }

    fun getGames(): List<String> = leaderboards.map { it.gameName }

    fun addPlayer(gameName: String, player: Player) {
        val game = leaderboards.find { it.gameName == gameName }
        game?.players?.add(player)
    }

    fun updatePoints(gameName: String, username: String, points: Int) {
        val player = leaderboards.find { it.gameName == gameName }?.players?.find { it.username == username }
        player?.points = points
    }

    fun recordMatch(gameName: String, username: String, result: MatchResult) {
        val player = leaderboards.find { it.gameName == gameName }?.players?.find { it.username == username } ?: return

        when (result) {
            MatchResult.WIN -> {
                player.wins++
                player.points += 3  // 3 points per win
            }
            MatchResult.LOSS -> {
                player.losses++
                // no points for loss
            }
            MatchResult.TIE -> {
                player.ties++
                player.points += 1  // 1 point per tie
            }
        }
    }

    enum class MatchResult {
        WIN, LOSS, TIE
    }
}
