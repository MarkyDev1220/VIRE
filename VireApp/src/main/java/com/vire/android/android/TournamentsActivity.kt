package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vire.android.R

class TournamentsActivity : BaseActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TournamentAdapter
    private lateinit var searchView: SearchView
    private lateinit var fabCreate: FloatingActionButton
    private lateinit var emptyText: TextView
    private lateinit var hamburgerButton: ImageButton

    private val games = listOf(
        "Magic: The Gathering",
        "Pokemon TCG",
        "Yu-Gi-Oh!",
        "Cardfight Vanguard (CFV)",
        "Lorcana",
        "Force of Will (FOW)",
        "Battle Spirits Saga (BSS)"
    )

    private val playerOptions = listOf(8, 16, 32) // even numbers: max 32

    // Replace with actual signed-in user
    private val currentUser = "demoUser"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournaments)

        hamburgerButton = findViewById(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener {
            val popup = android.widget.PopupMenu(this, it)
            popup.menu.add("Home")
            popup.menu.add("Profile")
            popup.menu.add("Messages")
            popup.menu.add("Buy/Sell")
            popup.menu.add("Challenges")
            popup.menu.add("Quest")
            popup.menu.add("Settings")
            popup.menu.add("Tournaments")
            popup.menu.add("Rankings")
            popup.menu.add("Friends")
            popup.menu.add("Search")

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

        searchView = findViewById(R.id.searchViewTournaments)
        fabCreate = findViewById(R.id.fabCreateTournament)
        emptyText = findViewById(R.id.emptyTextTournaments)
        recycler = findViewById(R.id.recyclerTournaments)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = TournamentAdapter(TournamentManager.getTournaments()) { item, action ->
            when (action) {
                TournamentAdapter.Action.VIEW -> showTournamentDetails(item)
                TournamentAdapter.Action.EDIT -> confirmEdit(item)
                TournamentAdapter.Action.DELETE -> confirmDelete(item)
            }
        }
        recycler.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { filterAndSearch(); return true }
            override fun onQueryTextChange(newText: String?): Boolean { filterAndSearch(); return true }
        })

        fabCreate.setOnClickListener { openCreateDialog() }

        refreshList()
    }

    private fun refreshList() {
        val list = TournamentManager.getTournaments()
        adapter.submitList(list)
        emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterAndSearch() {
        val q = searchView.query?.toString() ?: ""
        val filtered = TournamentManager.search(q)
        adapter.submitList(filtered)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showTournamentDetails(item: TournamentRequest) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(item.name)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_tournament_details, null)

        val tvName = view.findViewById<TextView>(R.id.detailTournamentName)
        val tvGame = view.findViewById<TextView>(R.id.detailTournamentGame)
        val tvPlayers = view.findViewById<TextView>(R.id.detailTournamentPlayers)
        val tvBracket = view.findViewById<TextView>(R.id.detailTournamentBracket)
        val tvPrizes = view.findViewById<TextView>(R.id.detailTournamentPrizes)
        val tvOrganizer = view.findViewById<TextView>(R.id.detailTournamentOrganizer)
        val tvCreated = view.findViewById<TextView>(R.id.detailTournamentCreated)

        tvName.text = item.name
        tvGame.text = item.game
        tvPlayers.text = "Min players: ${item.minPlayers} (max 32)"
        tvBracket.text = item.bracketLink
        tvPrizes.text = item.prizesDescription ?: "No prizes listed"
        tvOrganizer.text = "Organizer: ${item.organizer}"
        tvCreated.text = item.prettyCreatedAt()

        builder.setView(view)
        builder.setPositiveButton("Close", null)
        builder.show()
    }

    private fun confirmDelete(item: TournamentRequest) {
        if (!item.organizer.equals(currentUser, ignoreCase = true)) {
            Toast.makeText(this, "Only the organizer can delete this tournament.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete Tournament")
            .setMessage("Delete tournament '${item.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                TournamentManager.deleteTournament(item.id)
                filterAndSearch()
                Toast.makeText(this, "Tournament deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmEdit(item: TournamentRequest) {
        if (!item.organizer.equals(currentUser, ignoreCase = true)) {
            Toast.makeText(this, "Only the organizer can edit this tournament.", Toast.LENGTH_SHORT).show()
            return
        }
        openEditDialog(item)
    }

    private fun openEditDialog(item: TournamentRequest) {
        // reuse create dialog layout for edits
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_tournament, null)
        val spPlayers = view.findViewById<Spinner>(R.id.createTournamentPlayers)
        val spGame = view.findViewById<Spinner>(R.id.createTournamentGame)
        val etName = view.findViewById<EditText>(R.id.createTournamentName)
        val etBracket = view.findViewById<EditText>(R.id.createTournamentBracket)
        val etPrizes = view.findViewById<EditText>(R.id.createTournamentPrizes)

        spPlayers.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, playerOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spGame.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // prefill
        etName.setText(item.name)
        etBracket.setText(item.bracketLink)
        etPrizes.setText(item.prizesDescription ?: "")

        val playerPos = playerOptions.indexOf(item.minPlayers).coerceAtLeast(0)
        spPlayers.setSelection(playerPos)
        val gamePos = games.indexOf(item.game).coerceAtLeast(0)
        spGame.setSelection(gamePos)

        val builder = AlertDialog.Builder(this)
            .setTitle("Edit Tournament")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val players = spPlayers.selectedItem as Int
                val game = spGame.selectedItem as String
                val bracket = etBracket.text.toString().trim()
                val prizes = etPrizes.text.toString().trim().ifEmpty { null }

                // validation
                if (name.isEmpty()) { Toast.makeText(this, "Tournament name required", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                if (bracket.isEmpty() || !Patterns.WEB_URL.matcher(bracket).matches()) {
                    Toast.makeText(this, "A valid bracket link is required", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                }
                if (players !in playerOptions) {
                    Toast.makeText(this, "Invalid player count", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                }

                // if prizes points specified as "Points:4" - ensure between 4 and 6
                if (prizes != null && prizes.startsWith("Points:", ignoreCase = true)) {
                    val pts = prizes.substringAfter(":", "").toIntOrNull()
                    if (pts == null || pts < 4 || pts > 6) {
                        Toast.makeText(this, "Points must be between 4 and 6", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                    }
                }

                val updated = item.copy(
                    name = name,
                    game = game,
                    minPlayers = players,
                    bracketLink = bracket,
                    prizesDescription = prizes
                )

                val ok = TournamentManager.updateTournament(updated)
                if (ok) {
                    filterAndSearch()
                    Toast.makeText(this, "Tournament updated.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update tournament.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun openCreateDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_tournament, null)
        val spPlayers = view.findViewById<Spinner>(R.id.createTournamentPlayers)
        val spGame = view.findViewById<Spinner>(R.id.createTournamentGame)
        val etName = view.findViewById<EditText>(R.id.createTournamentName)
        val etBracket = view.findViewById<EditText>(R.id.createTournamentBracket)
        val etPrizes = view.findViewById<EditText>(R.id.createTournamentPrizes)

        spPlayers.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, playerOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spGame.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Create Tournament")
            .setView(view)
            .setPositiveButton("Create") { _, _ ->
                val name = etName.text.toString().trim()
                val players = spPlayers.selectedItem as Int
                val game = spGame.selectedItem as String
                val bracket = etBracket.text.toString().trim()
                val prizes = etPrizes.text.toString().trim().ifEmpty { null }

                // validations
                if (name.isEmpty()) { Toast.makeText(this, "Tournament name is required", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                if (bracket.isEmpty() || !Patterns.WEB_URL.matcher(bracket).matches()) {
                    Toast.makeText(this, "A valid bracket link is required", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                }
                if (players !in playerOptions) {
                    Toast.makeText(this, "Select a valid minimum number of players", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                }
                // prizes points validation if used
                if (prizes != null && prizes.startsWith("Points:", ignoreCase = true)) {
                    val pts = prizes.substringAfter(":", "").toIntOrNull()
                    if (pts == null || pts < 4 || pts > 6) {
                        Toast.makeText(this, "Points must be between 4 and 6", Toast.LENGTH_SHORT).show(); return@setPositiveButton
                    }
                }

                val t = TournamentRequest(
                    id = TournamentManager.nextId(),
                    name = name,
                    game = game,
                    minPlayers = players,
                    bracketLink = bracket,
                    prizesDescription = prizes,
                    organizer = currentUser
                )
                TournamentManager.addTournament(t)
                filterAndSearch()
                Toast.makeText(this, "Tournament created.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
        builder.show()
    }
}