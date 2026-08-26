package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vire.android.R

class ChallengesActivity : BaseActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChallengeAdapter
    private lateinit var spinnerGame: Spinner
    private lateinit var searchView: SearchView
    private lateinit var fabCreate: FloatingActionButton
    private lateinit var emptyText: TextView
    private lateinit var hamburgerButton: ImageButton

    private val games = listOf(
        "All",
        "Magic: The Gathering",
        "Pokemon TCG",
        "Yu-Gi-Oh!",
        "Battle Spirits Saga (BSS)",
        "Cardfight Vanguard (CFV)",
        "Lorcana",
        "Force of Will (FOW)"
    )

    // Replace this with your authenticated user id when available
    private val currentUser = "demoUser"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenges)

        // Header/hamburger
        hamburgerButton = findViewById(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener {
            val popup = PopupMenu(this, it)
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

        findViewById<TextView>(R.id.challengesText).text = "Challenges"

        recycler = findViewById(R.id.recyclerChallenges)
        spinnerGame = findViewById(R.id.spinnerGame)
        searchView = findViewById(R.id.searchViewChallenges)
        fabCreate = findViewById(R.id.fabCreateChallenge)
        emptyText = findViewById(R.id.emptyTextChallenges)

        // Recycler
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ChallengeAdapter(ChallengeManager.getChallenges()) { item, action ->
            when (action) {
                ChallengeAdapter.Action.VIEW -> showChallengeDetails(item)
                ChallengeAdapter.Action.ACCEPT -> confirmAccept(item)
                ChallengeAdapter.Action.EDIT -> confirmEdit(item)
                ChallengeAdapter.Action.DELETE -> confirmDelete(item)
            }
        }
        recycler.adapter = adapter

        // Spinner
        spinnerGame.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerGame.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterAndSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { filterAndSearch(); return true }
            override fun onQueryTextChange(newText: String?): Boolean { filterAndSearch(); return true }
        })

        // FAB create
        fabCreate.setOnClickListener { openCreateDialog() }

        // Notification channel
        NotificationHelper.ensureChannel(this)

        refreshList()
    }

    private fun refreshList() {
        val list = ChallengeManager.getChallenges()
        adapter.submitList(list)
        emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterAndSearch() {
        val q = searchView.query?.toString() ?: ""
        val game = spinnerGame.selectedItem?.toString() ?: "All"
        val filtered = ChallengeManager.searchAndFilter(q, game)
        adapter.submitList(filtered)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showChallengeDetails(item: ChallengeRequest) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(item.title)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_challenge_details, null)
        view.apply {
            findViewById<TextView>(R.id.detailGame).text = item.game
            findViewById<TextView>(R.id.detailChallenger).text = "Challenger: ${item.challenger}"
            findViewById<TextView>(R.id.detailOpponent).text = "Opponent: ${item.opponent ?: "Open"}"
            findViewById<TextView>(R.id.detailDesc).text = item.description
            findViewById<TextView>(R.id.detailStake).text = "Stake: ${item.stake}"
            findViewById<TextView>(R.id.detailCreated).text = item.prettyCreatedAt(this@ChallengesActivity)
        }
        builder.setView(view)
        builder.setPositiveButton("Close", null)
        builder.show()
    }

    private fun confirmAccept(item: ChallengeRequest) {
        if (item.challenger.equals(currentUser, ignoreCase = true)) {
            Toast.makeText(this, "You cannot accept your own challenge.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Accept Challenge")
            .setMessage("Accept challenge '${item.title}' by ${item.challenger}?")
            .setPositiveButton("Accept") { _, _ ->
                ChallengeManager.acceptChallenge(item.id, currentUser)
                filterAndSearch()
                Toast.makeText(this, "Challenge accepted (demo).", Toast.LENGTH_SHORT).show()

                val notif = NotificationItem(
                    id = NotificationStore.nextId(),
                    toUser = item.challenger,
                    title = "Your challenge was accepted",
                    message = "$currentUser accepted your challenge '${item.title}'"
                )
                NotificationStore.addNotification(notif)
                NotificationHelper.showSystemNotification(this, notif.id.toInt(), notif.title, notif.message)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(item: ChallengeRequest) {
        if (!item.challenger.equals(currentUser, ignoreCase = true)) {
            Toast.makeText(this, "Only the challenger can delete this post.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete Challenge")
            .setMessage("Delete challenge '${item.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                ChallengeManager.deleteChallenge(item.id)
                filterAndSearch()
                Toast.makeText(this, "Challenge deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmEdit(item: ChallengeRequest) {
        if (!item.challenger.equals(currentUser, ignoreCase = true)) {
            Toast.makeText(this, "Only the challenger can edit this post.", Toast.LENGTH_SHORT).show()
            return
        }
        openEditDialog(item)
    }

    private fun openEditDialog(item: ChallengeRequest) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_challenge, null)
        val spGame = view.findViewById<Spinner>(R.id.createGame)
        val acOpponent = view.findViewById<AutoCompleteTextView>(R.id.createOpponent)
        val etTitle = view.findViewById<EditText>(R.id.createTitle)
        val etDesc = view.findViewById<EditText>(R.id.createDescription)
        val etStake = view.findViewById<EditText>(R.id.createStake)

        val gameList = games.drop(1)
        spGame.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gameList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        etTitle.setText(item.title)
        etDesc.setText(item.description)
        etStake.setText(item.stake)
        spGame.setSelection(gameList.indexOf(item.game).coerceAtLeast(0))
        acOpponent.setText(item.opponent ?: "")

        // --- FIX: fetch usernames only ---
        val initialUsers = ArrayList(UserManager.getAllUserObjects().map { it.username })
        val userAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, initialUsers)
        acOpponent.setAdapter(userAdapter)
        acOpponent.threshold = 1
        acOpponent.setOnClickListener { acOpponent.showDropDown() }

        acOpponent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString() ?: ""
                val results = UserManager.searchUsers(q)
                acOpponent.post {
                    userAdapter.clear()
                    for (user in results) {
                        userAdapter.add(user.username)  // <-- use the username string
                    }
                    userAdapter.notifyDataSetChanged()

                    if (results.isNotEmpty()) try { acOpponent.showDropDown() } catch (_: Exception) {}
                }
            }
        })

        AlertDialog.Builder(this)
            .setTitle("Edit Challenge")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString().trim()
                val game = spGame.selectedItem?.toString() ?: gameList[0]
                val opponentTextRaw = acOpponent.text.toString().trim().ifEmpty { null }
                val opponentText = opponentTextRaw?.takeIf { it.isNotBlank() }
                val desc = etDesc.text.toString().trim()
                val stake = etStake.text.toString().trim().ifEmpty { "None" }

                if (title.isEmpty()) {
                    Toast.makeText(this, "Please add a title", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (opponentText != null && !UserManager.userExists(opponentText)) {
                    Toast.makeText(this, "Opponent user not found: $opponentText", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updated = item.copy(
                    title = title,
                    game = game,
                    opponent = opponentText,
                    description = desc,
                    stake = stake
                )

                if (ChallengeManager.updateChallenge(updated)) {
                    filterAndSearch()
                    Toast.makeText(this, "Challenge updated.", Toast.LENGTH_SHORT).show()

                    if (opponentText != null && !opponentText.equals(item.opponent, ignoreCase = true)) {
                        val notif = NotificationItem(
                            id = NotificationStore.nextId(),
                            toUser = opponentText,
                            title = "You were challenged",
                            message = "$currentUser updated a challenge for you: '${updated.title}'"
                        )
                        NotificationStore.addNotification(notif)
                        NotificationHelper.showSystemNotification(this, notif.id.toInt(), notif.title, notif.message)
                    }
                } else {
                    Toast.makeText(this, "Failed to update challenge.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openCreateDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_challenge, null)
        val spGame = view.findViewById<Spinner>(R.id.createGame)
        val acOpponent = view.findViewById<AutoCompleteTextView>(R.id.createOpponent)
        val etTitle = view.findViewById<EditText>(R.id.createTitle)
        val etDesc = view.findViewById<EditText>(R.id.createDescription)
        val etStake = view.findViewById<EditText>(R.id.createStake)

        spGame.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games.drop(1)).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val initialUsers = ArrayList(UserManager.getAllUserObjects().map { it.username })
        val userAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, initialUsers)
        acOpponent.setAdapter(userAdapter)
        acOpponent.threshold = 1
        acOpponent.setOnClickListener { acOpponent.showDropDown() }

        acOpponent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString() ?: ""
                val results = UserManager.searchUsers(q)
                acOpponent.post {
                    userAdapter.clear()
                    for (user in results) {
                        userAdapter.add(user.username)
                    }
                    userAdapter.notifyDataSetChanged()

                    if (results.isNotEmpty()) try { acOpponent.showDropDown() } catch (_: Exception) {}
                }
            }
        })

        AlertDialog.Builder(this)
            .setTitle("Create Challenge")
            .setView(view)
            .setPositiveButton("Post") { _, _ ->
                val title = etTitle.text.toString().trim()
                val game = spGame.selectedItem?.toString() ?: games[1]
                val opponentTextRaw = acOpponent.text.toString().trim().ifEmpty { null }
                val opponentText = opponentTextRaw?.takeIf { it.isNotBlank() }
                val desc = etDesc.text.toString().trim()
                val stake = etStake.text.toString().trim().ifEmpty { "None" }
                val challenger = currentUser

                if (title.isEmpty()) {
                    Toast.makeText(this, "Please add a title", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (opponentText != null && !UserManager.userExists(opponentText)) {
                    Toast.makeText(this, "Opponent user not found: $opponentText", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val challenge = ChallengeRequest(
                    id = ChallengeManager.nextId(),
                    title = title,
                    game = game,
                    challenger = challenger,
                    opponent = opponentText,
                    description = desc,
                    stake = stake
                )
                ChallengeManager.addChallenge(challenge)
                filterAndSearch()
                Toast.makeText(this, "Challenge posted.", Toast.LENGTH_SHORT).show()

                if (opponentText != null) {
                    val notif = NotificationItem(
                        id = NotificationStore.nextId(),
                        toUser = opponentText,
                        title = "You were challenged",
                        message = "$challenger challenged you to '$title'"
                    )
                    NotificationStore.addNotification(notif)
                    NotificationHelper.showSystemNotification(this, notif.id.toInt(), notif.title, notif.message)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
