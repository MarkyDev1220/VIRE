package com.vire.android.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vire.android.R

class RankingsActivity : BaseActivity() {

    private lateinit var gameSpinner: Spinner
    private lateinit var recyclerLeaderboard: RecyclerView
    private lateinit var hamburgerButton: ImageButton
    private lateinit var fabRecord: FloatingActionButton

    private lateinit var recyclerAdapter: LeaderboardAdapter
    private lateinit var adapter: ArrayAdapter<String> // legacy ListView fallback
    private lateinit var leaderboardList: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rankings)

        // Views
        gameSpinner = findViewById(R.id.gameSpinner)
        recyclerLeaderboard = findViewById(R.id.recyclerLeaderboard)
        hamburgerButton = findViewById(R.id.hamburgerButton)
        fabRecord = findViewById(R.id.fabRecord)
        leaderboardList = findViewById(R.id.leaderboardList)

        val games = LeaderboardManager.getGames()

        // Spinner setup
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        gameSpinner.adapter = spinnerAdapter

        // RecyclerView setup
        recyclerLeaderboard.layoutManager = LinearLayoutManager(this)
        val initialPlayers = LeaderboardManager.getLeaderboard(games[0])
        recyclerAdapter = LeaderboardAdapter(initialPlayers)
        recyclerLeaderboard.adapter = recyclerAdapter

        // Legacy ListView fallback
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            initialPlayers.mapIndexed { index, player -> "${index + 1}. ${player.username} - ${player.points} pts" }
        )
        leaderboardList.adapter = adapter
        leaderboardList.visibility = View.GONE

        // Spinner listener
        gameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedGame = games[position]
                val players = LeaderboardManager.getLeaderboard(selectedGame)
                recyclerAdapter.updateLeaderboard(players)

                adapter.clear()
                adapter.addAll(players.mapIndexed { index, player -> "${index + 1}. ${player.username} - ${player.points} pts" })
                adapter.notifyDataSetChanged()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Hamburger menu
        hamburgerButton.setOnClickListener { view -> showHamburgerMenu(view) }

        // FAB: Record Win/Loss/Tie
        fabRecord.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            listOf("Record a Win", "Record a Loss", "Record a Tie").forEach { popup.menu.add(it) }

            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Record a Win" -> showRecordDialog("win")
                    "Record a Loss" -> showRecordDialog("loss")
                    "Record a Tie" -> showRecordDialog("tie")
                }
                true
            }
            popup.show()
        }
    }

    /** Dialog to select player and record result */
    private fun showRecordDialog(type: String) {
        val selectedGame = gameSpinner.selectedItem.toString()
        val players = LeaderboardManager.getLeaderboard(selectedGame)
        val playerNames = players.map { it.username }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Player")
            .setItems(playerNames) { _, which ->
                val player = players[which]

                // Opponent selection
                val opponents = players.filter { it.username != player.username }.map { it.username }.toTypedArray()
                if (opponents.isNotEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("Select Opponent")
                        .setItems(opponents) { _, oppIndex ->
                            val opponent = opponents[oppIndex]

                            // Update player stats
                            when (type.lowercase()) {
                                "win" -> { player.wins++; player.points += 6 }
                                "loss" -> { player.losses++; player.points += 3 }
                                "tie" -> { player.ties++; player.points += 2 }
                            }

                            // Update leaderboard UI
                            recyclerAdapter.updateLeaderboard(players)
                            adapter.clear()
                            adapter.addAll(players.mapIndexed { index, p -> "${index + 1}. ${p.username} - ${p.points} pts" })
                            adapter.notifyDataSetChanged()

                            // Create feed post
                            GameResultManager.recordGameResult(player.username, type, opponent)
                        }
                        .show()
                } else {
                    // No opponent available
                    Toast.makeText(this, "No opponent available to record against", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showHamburgerMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        listOf("Home", "Profile", "Messages", "Buy/Sell", "Challenges", "Quest", "Settings", "Tournaments", "Rankings", "Friends", "Search")
            .forEach { popup.menu.add(it) }

        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                "Profile" -> startActivity(Intent(this, ProfileActivity::class.java))
                "Messages" -> startActivity(Intent(this, MessagesActivity::class.java))
                "Buy/Sell" -> startActivity(Intent(this, BuySellActivity::class.java))
                "Challenges" -> startActivity(Intent(this, ChallengesActivity::class.java))
                "Quest" -> startActivity(Intent(this, QuestActivity::class.java))
                "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                "Tournaments" -> startActivity(Intent(this, TournamentsActivity::class.java))
                "Rankings" -> startActivity(Intent(this, RankingsActivity::class.java))
                "Friends" -> startActivity(Intent(this, FriendsActivity::class.java))
                "Search" -> startActivity(Intent(this, SearchActivity::class.java))
            }
            true
        }
        popup.show()
    }
}



