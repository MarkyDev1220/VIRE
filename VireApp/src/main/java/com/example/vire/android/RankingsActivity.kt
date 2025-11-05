package com.example.vire.android

import android.content.Intent
import android.os.Bundle
import android.widget.*


class RankingsActivity : BaseActivity() {

    private lateinit var gameSpinner: Spinner
    private lateinit var leaderboardList: ListView
    private lateinit var hamburgerButton: ImageButton

    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rankings)

        gameSpinner = findViewById(R.id.gameSpinner)
        leaderboardList = findViewById(R.id.leaderboardList)
        hamburgerButton = findViewById(R.id.hamburgerButton)

        val games = LeaderboardManager.getGames()

        // Set up spinner
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        gameSpinner.adapter = spinnerAdapter

        // Initial leaderboard
        val initialPlayers = LeaderboardManager.getLeaderboard(games[0])
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            initialPlayers.mapIndexed { index, player -> "${index + 1}. ${player.username} - ${player.points} pts" }
        )
        leaderboardList.adapter = adapter

        // Spinner selection listener
        gameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedGame = games[position]
                val players = LeaderboardManager.getLeaderboard(selectedGame)
                adapter.clear()
                adapter.addAll(players.mapIndexed { index, player -> "${index + 1}. ${player.username} - ${player.points} pts" })
                adapter.notifyDataSetChanged()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Hamburger menu
        hamburgerButton.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("Home")
            popup.menu.add("Profile")
            popup.menu.add("Messages")
            popup.menu.add("Buy/Sell")
            popup.menu.add("Challenges")
            popup.menu.add("Quest")
            popup.menu.add("Settings")
            popup.menu.add("Tournaments")
            popup.menu.add("Rankings")

            popup.setOnMenuItemClickListener { item ->
                when(item.title.toString()) {
                    "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                    "Profile" -> startActivity(Intent(this, ProfileActivity::class.java))
                    "Messages" -> startActivity(Intent(this, MessagesActivity::class.java))
                    "Buy/Sell" -> startActivity(Intent(this, BuySellActivity::class.java))
                    "Challenges" -> startActivity(Intent(this, ChallengesActivity::class.java))
                    "Quest" -> startActivity(Intent(this, QuestActivity::class.java))
                    "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                    "Tournaments" -> startActivity(Intent(this, TournamentsActivity::class.java))
                    "Rankings" -> startActivity(Intent(this, RankingsActivity::class.java))
                }
                true
            }
            popup.show()
        }
    }
}
