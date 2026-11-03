package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.vire.android.R

class DeckListActivity : BaseActivity() {

    private lateinit var rgScope: RadioGroup
    private lateinit var rbMyDecks: RadioButton
    private lateinit var rbPublicDecks: RadioButton
    private lateinit var spinnerGame: Spinner
    private lateinit var recycler: RecyclerView
    private lateinit var fabCreate: FloatingActionButton
    private lateinit var emptyText: TextView
    private lateinit var countText: TextView
    private lateinit var searchInput: EditText
    private lateinit var adapter: DeckAdapter
    private var searchQuery = ""

    private val gameSystems = listOf(
        "All",
        "Magic: The Gathering",
        "Pokémon",
        "Yu-Gi-Oh!",
        "Disney Lorcana",
        "Cardfight Vanguard (CFV)",
        "Battle Spirits Saga (BSS)",
        "Force of Will (FOW)"
    )

    private val createGameSystems = listOf(
        "Magic: The Gathering",
        "Pokémon",
        "Yu-Gi-Oh!",
        "Disney Lorcana",
        "Cardfight Vanguard (CFV)",
        "Battle Spirits Saga (BSS)",
        "Force of Will (FOW)"
    )

    private var allDecks = listOf<Deck>()

    private var pendingImageUri: Uri? = null
    private var currentDialog: AlertDialog? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImageUri = it
            val iv = currentDialog?.findViewById<ImageView>(R.id.deckImagePreview)
            iv?.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_list)

        setupStandardBottomNav(BottomTab.DECKS)

        rgScope = findViewById(R.id.rgDeckScope)
        rbMyDecks = findViewById(R.id.rbMyDecks)
        rbPublicDecks = findViewById(R.id.rbPublicDecks)
        spinnerGame = findViewById(R.id.spinnerDeckGameFilter)
        recycler = findViewById(R.id.recyclerDecks)
        fabCreate = findViewById(R.id.fabCreateDeck)
        emptyText = findViewById(R.id.emptyDecksText)
        countText = findViewById(R.id.deckCountText)
        searchInput = findViewById(R.id.searchDecks)

        findViewById<ImageButton>(R.id.btnDeckNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnDeckProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString().orEmpty().trim()
                applyDeckSearch()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = DeckAdapter(
            initial = emptyList(),
            onDeckClick = { deck ->
                val intent = Intent(this, DeckDetailsActivity::class.java)
                intent.putExtra("deck", deck)
                startActivity(intent)
            },
            onOptionClick = { deck, action ->
                when (action) {
                    DeckAdapter.Action.EDIT -> openAddEditDeckDialog(deck)
                    DeckAdapter.Action.DELETE -> confirmDeleteDeck(deck)
                    else -> {}
                }
            }
        )
        recycler.adapter = adapter

        spinnerGame.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gameSystems).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerGame.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadDecks()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        rgScope.setOnCheckedChangeListener { _, _ ->
            loadDecks()
        }

        fabCreate.setOnClickListener {
            openAddEditDeckDialog(null)
        }

        loadDecks()
    }

    override fun onResume() {
        super.onResume()
        loadDecks()
    }

    private fun loadDecks() {
        val selectedGame = spinnerGame.selectedItem?.toString() ?: "All"
        val isMyDecks = rbMyDecks.isChecked

        if (isMyDecks) {
            DeckManager.getUserDecks { list ->
                val filtered = if (selectedGame.equals("All", ignoreCase = true)) {
                    list
                } else {
                    list.filter { it.gameSystem.equals(selectedGame, ignoreCase = true) }
                }
                allDecks = filtered
                updateDeckMetrics(filtered)
                applyDeckSearch()
            }
        } else {
            DeckManager.getPublicDecks(selectedGame) { list ->
                allDecks = list
                updateDeckMetrics(list)
                applyDeckSearch()
            }
        }
    }

    private fun updateDeckMetrics(decks: List<Deck>) {
        findViewById<TextView>(R.id.tvDeckMetricCount).text = decks.size.toString()
        val cardsOwned = decks.sumOf { deck ->
            deck.cards.sumOf { card -> card.quantity.coerceAtLeast(0) }
        }
        findViewById<TextView>(R.id.tvCardsOwned).text = cardsOwned.toString()
    }

    private fun applyDeckSearch() {
        val query = searchQuery
        val filtered = if (query.isBlank()) allDecks else allDecks.filter { deck ->
            deck.deckName.contains(query, ignoreCase = true) ||
                    deck.gameSystem.contains(query, ignoreCase = true) ||
                    deck.format.contains(query, ignoreCase = true) ||
                    deck.cards.any { it.cardName.contains(query, ignoreCase = true) }
        }
        adapter.submitList(filtered)
        countText.text = "${filtered.size} decks"
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openAddEditDeckDialog(existingDeck: Deck?) {
        val isEdit = existingDeck != null
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_deck, null)

        val etName = view.findViewById<EditText>(R.id.editDeckName)
        val spGame = view.findViewById<Spinner>(R.id.spinnerDeckGame)
        val etFormat = view.findViewById<EditText>(R.id.editDeckFormat)
        val etDesc = view.findViewById<EditText>(R.id.editDeckDescription)
        val etDiscord = view.findViewById<EditText>(R.id.editDeckDiscordUrl)
        val cbPublic = view.findViewById<CheckBox>(R.id.checkDeckPublic)
        val ivPreview = view.findViewById<ImageView>(R.id.deckImagePreview)
        val btnChoosePhoto = view.findViewById<Button>(R.id.btnPickDeckPhoto)

        spGame.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, createGameSystems).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        pendingImageUri = null

        if (existingDeck != null) {
            etName.setText(existingDeck.deckName)
            etFormat.setText(existingDeck.format)
            etDesc.setText(existingDeck.description)
            etDiscord.setText(existingDeck.discordUrl)
            cbPublic.isChecked = existingDeck.isPublic

            val gameIndex = createGameSystems.indexOf(existingDeck.gameSystem)
            if (gameIndex >= 0) spGame.setSelection(gameIndex)

            if (existingDeck.coverImageUrl.isNotBlank()) {
                if (existingDeck.coverImageUrl.startsWith("http://") || existingDeck.coverImageUrl.startsWith("https://")) {
                    Picasso.get().load(existingDeck.coverImageUrl).into(ivPreview)
                } else {
                    try {
                        ivPreview.setImageURI(Uri.parse(existingDeck.coverImageUrl))
                    } catch (e: Exception) {
                        ivPreview.setImageResource(R.drawable.ic_placeholder)
                    }
                }
            }
        }

        btnChoosePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val username = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", "Gamer") ?: "Gamer"

        val builder = AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Deck" else "Create TCG Deck")
            .setView(view)
            .setPositiveButton(if (isEdit) "Save" else "Create Deck") { _, _ ->
                val deckName = etName.text.toString().trim()
                if (deckName.isEmpty()) {
                    Toast.makeText(this, "Deck name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val gameSystem = spGame.selectedItem?.toString() ?: "Magic: The Gathering"
                val format = etFormat.text.toString().trim().ifBlank { "Standard" }
                val description = etDesc.text.toString().trim()
                val discordUrl = etDiscord.text.toString().trim()
                val isPublic = cbPublic.isChecked
                val coverImageUrl = pendingImageUri?.toString() ?: existingDeck?.coverImageUrl ?: ""

                val deck = Deck(
                    id = existingDeck?.id ?: "",
                    userId = existingDeck?.userId ?: "",
                    username = username,
                    deckName = deckName,
                    gameSystem = gameSystem,
                    format = format,
                    description = description,
                    cards = existingDeck?.cards ?: emptyList(),
                    cardCount = existingDeck?.cards?.sumOf { it.quantity } ?: 0,
                    coverImageUrl = coverImageUrl,
                    discordUrl = discordUrl,
                    isPublic = isPublic
                )

                if (isEdit) {
                    DeckManager.updateDeck(deck) { success ->
                        if (success) {
                            Toast.makeText(this, "Deck updated!", Toast.LENGTH_SHORT).show()
                            loadDecks()
                        }
                    }
                } else {
                    DeckManager.createDeck(deck) { success, _ ->
                        if (success) {
                            Toast.makeText(this, "Deck created!", Toast.LENGTH_SHORT).show()
                            loadDecks()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()
        currentDialog = dialog
        dialog.show()
    }

    private fun confirmDeleteDeck(deck: Deck) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${deck.deckName}?")
            .setMessage("Are you sure you want to delete this deck list?")
            .setPositiveButton("Delete") { _, _ ->
                DeckManager.deleteDeck(deck.id) { success ->
                    if (success) {
                        Toast.makeText(this, "Deck deleted", Toast.LENGTH_SHORT).show()
                        loadDecks()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
