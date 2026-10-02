package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.vire.android.R

class TournamentsActivity : BaseActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TournamentAdapter
    private lateinit var searchView: SearchView
    private lateinit var fabCreate: FloatingActionButton
    private lateinit var emptyText: TextView

    private val games = listOf(
        "Magic: The Gathering",
        "Pokémon TCG",
        "Yu-Gi-Oh!",
        "Lorcana",
        "Warhammer",
        "D&D",
        "Board Games",
        "Other"
    )

    private val formats = listOf(
        "Swiss",
        "Single Elimination",
        "Double Elimination",
        "Casual / Open Play"
    )

    private var allTournaments = listOf<Tournament>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournaments)

        setupHamburgerMenu()

        searchView = findViewById(R.id.searchViewTournaments)
        fabCreate = findViewById(R.id.fabCreateTournament)
        emptyText = findViewById(R.id.emptyTextTournaments)
        recycler = findViewById(R.id.recyclerTournaments)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = TournamentAdapter(
            initial = emptyList(),
            currentUid = currentUid,
            onTournamentClick = { tournament ->
                val intent = Intent(this, TournamentDetailsActivity::class.java)
                intent.putExtra("tournament", tournament)
                startActivity(intent)
            },
            onRegisterClick = { tournament ->
                handleRegisterToggle(tournament)
            }
        )
        recycler.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterTournaments()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTournaments()
                return true
            }
        })

        fabCreate.setOnClickListener {
            openCreateTournamentDialog()
        }

        loadTournaments()
    }

    override fun onResume() {
        super.onResume()
        loadTournaments()
    }

    private fun loadTournaments() {
        TournamentManager.getTournaments { list ->
            allTournaments = list
            filterTournaments()
        }
    }

    private fun filterTournaments() {
        val query = searchView.query?.toString() ?: ""
        val filtered = TournamentManager.searchTournaments(query, allTournaments)
        adapter.submitList(filtered)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun handleRegisterToggle(tournament: Tournament) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
        val isRegistered = tournament.participants.contains(currentUid)

        if (isRegistered) {
            TournamentManager.unregisterPlayer(tournament.id, currentUid) { success ->
                if (success) {
                    Toast.makeText(this, "Unregistered from tournament", Toast.LENGTH_SHORT).show()
                    loadTournaments()
                }
            }
        } else {
            if (tournament.participants.size >= tournament.maxParticipants) {
                Toast.makeText(this, "Tournament is full!", Toast.LENGTH_SHORT).show()
                return
            }

            TournamentManager.registerPlayer(tournament.id, currentUid) { success ->
                if (success) {
                    Toast.makeText(this, "Successfully registered for tournament!", Toast.LENGTH_SHORT).show()
                    loadTournaments()
                }
            }
        }
    }

    private fun openCreateTournamentDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_tournament, null)

        val etTitle = view.findViewById<EditText>(R.id.editTournamentTitle)
        val spGame = view.findViewById<Spinner>(R.id.spinnerTournamentGame)
        val spFormat = view.findViewById<Spinner>(R.id.spinnerTournamentFormat)
        val etDate = view.findViewById<EditText>(R.id.editTournamentDate)
        val etTime = view.findViewById<EditText>(R.id.editTournamentTime)
        val etLocation = view.findViewById<EditText>(R.id.editTournamentLocation)
        val etFee = view.findViewById<EditText>(R.id.editTournamentFee)
        val etMaxPlayers = view.findViewById<EditText>(R.id.editTournamentMaxPlayers)
        val etRules = view.findViewById<EditText>(R.id.editTournamentRules)
        val etPrizes = view.findViewById<EditText>(R.id.editTournamentPrizes)
        val etDiscord = view.findViewById<EditText>(R.id.editTournamentDiscord)
        val etBracket = view.findViewById<EditText>(R.id.editTournamentBracketLink)

        spGame.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spFormat.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, formats).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Create Tournament & Event")
            .setView(view)
            .setPositiveButton("Create Event") { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(this, "Event title is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val game = spGame.selectedItem?.toString() ?: "Magic: The Gathering"
                val format = spFormat.selectedItem?.toString() ?: "Swiss"
                val date = etDate.text.toString().trim()
                val time = etTime.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val fee = etFee.text.toString().trim().ifBlank { "Free" }
                val maxPlayers = etMaxPlayers.text.toString().toIntOrNull() ?: 16
                val rules = etRules.text.toString().trim()
                val prizes = etPrizes.text.toString().trim()
                val discordUrl = etDiscord.text.toString().trim()
                val bracketUrl = etBracket.text.toString().trim()

                val username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    .getString("username", "Host") ?: "Host"
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_host"

                val tournament = Tournament(
                    name = title,
                    gameSystem = game,
                    format = format,
                    date = date,
                    time = time,
                    location = location,
                    entryFee = fee,
                    maxParticipants = maxPlayers,
                    rules = rules,
                    prizesDescription = prizes,
                    discordUrl = discordUrl,
                    externalBracketUrl = bracketUrl,
                    organizer = username,
                    hostUid = uid,
                    status = "Upcoming"
                )

                TournamentManager.createTournament(tournament) { success, _ ->
                    if (success) {
                        Toast.makeText(this, "Tournament created successfully!", Toast.LENGTH_SHORT).show()
                        loadTournaments()
                    } else {
                        Toast.makeText(this, "Failed to create tournament", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)

        builder.show()
    }
}
