package com.vire.android.android

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
    private lateinit var fabRecord: FloatingActionButton

    private lateinit var recyclerAdapter: LeaderboardAdapter
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var leaderboardList: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rankings)

        setupHamburgerMenu()

        gameSpinner = findViewById(R.id.gameSpinner)
        recyclerLeaderboard = findViewById(R.id.recyclerLeaderboard)
        fabRecord = findViewById(R.id.fabRecord)
        leaderboardList = findViewById(R.id.leaderboardList)

        val games = LeaderboardManager.getGames()

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        gameSpinner.adapter = spinnerAdapter

        recyclerLeaderboard.layoutManager = LinearLayoutManager(this)
        val initialPlayers = LeaderboardManager.getLeaderboard(games[0])
        recyclerAdapter = LeaderboardAdapter(initialPlayers)
        recyclerLeaderboard.adapter = recyclerAdapter

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            initialPlayers.mapIndexed { index, player -> "${index + 1}. ${player.username} - ${player.points} pts" }
        )
        leaderboardList.adapter = adapter
        leaderboardList.visibility = View.GONE

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

    private fun showRecordDialog(type: String) {
        val selectedGame = gameSpinner.selectedItem.toString()
        val players = LeaderboardManager.getLeaderboard(selectedGame)
        val playerNames = players.map { it.username }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Player")
            .setItems(playerNames) { _, which ->
                val player = players[which]
                val opponents = players.filter { it.username != player.username }.map { it.username }.toTypedArray()
                if (opponents.isNotEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("Select Opponent")
                        .setItems(opponents) { _, oppIndex ->
                            val opponent = opponents[oppIndex]
                            when (type.lowercase()) {
                                "win" -> { player.wins++; player.points += 6 }
                                "loss" -> { player.losses++; player.points += 3 }
                                "tie" -> { player.ties++; player.points += 2 }
                            }
                            recyclerAdapter.updateLeaderboard(players)
                            adapter.clear()
                            adapter.addAll(players.mapIndexed { index, p -> "${index + 1}. ${p.username} - ${p.points} pts" })
                            adapter.notifyDataSetChanged()
                            GameResultManager.recordGameResult(player.username, type, opponent)
                        }
                        .show()
                } else {
                    Toast.makeText(this, "No opponent available", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
}
