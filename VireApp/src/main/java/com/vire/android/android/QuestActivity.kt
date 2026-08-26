package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vire.android.R

class QuestActivity : BaseActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: QuestAdapter
    private lateinit var searchView: SearchView
    private lateinit var filterTypeSpinner: Spinner
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

    // Replace with real signed-in user
    private val currentUser = "demoUser"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest)

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

        searchView = findViewById(R.id.searchViewQuest)
        filterTypeSpinner = findViewById(R.id.filterQuestType)
        fabCreate = findViewById(R.id.fabCreateQuest)
        emptyText = findViewById(R.id.emptyTextQuest)
        recycler = findViewById(R.id.recyclerQuest)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = QuestAdapter(QuestManager.getQuests()) { item, action ->
            when (action) {
                QuestAdapter.Action.VIEW -> showQuestDetails(item)
                QuestAdapter.Action.EDIT -> confirmEdit(item)
                QuestAdapter.Action.DELETE -> confirmDelete(item)
            }
        }
        recycler.adapter = adapter

        // filter types
        val types = listOf("All", QuestRequest.Type.MATCHES.name, QuestRequest.Type.ISO.name, QuestRequest.Type.TEST.name)
        filterTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        filterTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) { filterAndSearch() }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { filterAndSearch(); return true }
            override fun onQueryTextChange(newText: String?): Boolean { filterAndSearch(); return true }
        })

        fabCreate.setOnClickListener { openCreateDialog() }

        refreshList()
    }

    private fun refreshList() {
        val list = QuestManager.getQuests()
        adapter.submitList(list)
        emptyText.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun filterAndSearch() {
        val q = searchView.query?.toString() ?: ""
        val sel = filterTypeSpinner.selectedItem?.toString()
        val type = when (sel) {
            QuestRequest.Type.MATCHES.name -> QuestRequest.Type.MATCHES
            QuestRequest.Type.ISO.name -> QuestRequest.Type.ISO
            QuestRequest.Type.TEST.name -> QuestRequest.Type.TEST
            else -> null
        }
        val filtered = QuestManager.search(q, type)
        adapter.submitList(filtered)
        emptyText.visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun showQuestDetails(item: QuestRequest) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(item.title)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_quest_details, null)
        val tvTitle = view.findViewById<TextView>(R.id.detailQuestTitle)
        val tvType = view.findViewById<TextView>(R.id.detailQuestType)
        val tvGame = view.findViewById<TextView>(R.id.detailQuestGame)
        val tvDesc = view.findViewById<TextView>(R.id.detailQuestDescription)
        val tvDetails = view.findViewById<TextView>(R.id.detailQuestDetails)
        val tvPostedBy = view.findViewById<TextView>(R.id.detailQuestPoster)
        val tvCreated = view.findViewById<TextView>(R.id.detailQuestCreated)

        tvTitle.text = item.title
        tvType.text = item.type.name
        tvGame.text = item.game
        tvDesc.text = item.description
        tvDetails.text = item.details ?: "—"
        tvPostedBy.text = "Posted by: ${item.poster}"
        tvCreated.text = item.prettyCreatedAt()

        builder.setView(view)
        builder.setPositiveButton("Close", null)
        builder.show()
    }

    private fun confirmDelete(item: QuestRequest) {
        if (!item.poster.equals(currentUser, ignoreCase = true)) {
            Toast.makeText(this, "Only the poster can delete this quest.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete Quest")
            .setMessage("Delete quest '${item.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                QuestManager.deleteQuest(item.id)
                filterAndSearch()
                Toast.makeText(this, "Quest deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmEdit(item: QuestRequest) {
        if (!item.poster.equals(currentUser, ignoreCase = true)) {
            Toast.makeText(this, "Only the poster can edit this quest.", Toast.LENGTH_SHORT).show()
            return
        }
        openEditDialog(item)
    }

    private fun openEditDialog(item: QuestRequest) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_quest, null)
        val spType = view.findViewById<Spinner>(R.id.createQuestType)
        val spGame = view.findViewById<Spinner>(R.id.createQuestGame)
        val etTitle = view.findViewById<EditText>(R.id.createQuestTitle)
        val etDesc = view.findViewById<EditText>(R.id.createQuestDescription)
        val etDetails = view.findViewById<EditText>(R.id.createQuestDetails)

        val typeNames = listOf(QuestRequest.Type.MATCHES.name, QuestRequest.Type.ISO.name, QuestRequest.Type.TEST.name)
        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spGame.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // prefill
        etTitle.setText(item.title)
        etDesc.setText(item.description)
        etDetails.setText(item.details ?: "")
        spType.setSelection(typeNames.indexOf(item.type.name).coerceAtLeast(0))
        spGame.setSelection(games.indexOf(item.game).coerceAtLeast(0))

        val builder = AlertDialog.Builder(this)
            .setTitle("Edit Quest")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString().trim()
                val type = QuestRequest.Type.valueOf(spType.selectedItem as String)
                val game = spGame.selectedItem as String
                val desc = etDesc.text.toString().trim()
                val details = etDetails.text.toString().trim().ifEmpty { null }

                if (title.isEmpty()) { Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

                val updated = item.copy(
                    title = title,
                    type = type,
                    game = game,
                    description = desc,
                    details = details
                )
                val ok = QuestManager.updateQuest(updated)
                if (ok) {
                    filterAndSearch()
                    Toast.makeText(this, "Quest updated.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update quest.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun openCreateDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_quest, null)
        val spType = view.findViewById<Spinner>(R.id.createQuestType)
        val spGame = view.findViewById<Spinner>(R.id.createQuestGame)
        val etTitle = view.findViewById<EditText>(R.id.createQuestTitle)
        val etDesc = view.findViewById<EditText>(R.id.createQuestDescription)
        val etDetails = view.findViewById<EditText>(R.id.createQuestDetails)

        val typeNames = listOf(QuestRequest.Type.MATCHES.name, QuestRequest.Type.ISO.name, QuestRequest.Type.TEST.name)
        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spGame.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, games).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Create Quest")
            .setView(view)
            .setPositiveButton("Post") { _, _ ->
                val title = etTitle.text.toString().trim()
                val type = QuestRequest.Type.valueOf(spType.selectedItem as String)
                val game = spGame.selectedItem as String
                val desc = etDesc.text.toString().trim()
                val details = etDetails.text.toString().trim().ifEmpty { null }

                if (title.isEmpty()) { Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                if (desc.isEmpty()) { Toast.makeText(this, "Description is required", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

                val q = QuestRequest(
                    id = QuestManager.nextId(),
                    title = title,
                    type = type,
                    game = game,
                    description = desc,
                    poster = currentUser,
                    details = details
                )
                QuestManager.addQuest(q)
                filterAndSearch()
                Toast.makeText(this, "Quest posted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
        builder.show()
    }
}