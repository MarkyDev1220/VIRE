package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.vire.android.R

class DeckDetailsActivity : BaseActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvGameBadge: TextView
    private lateinit var tvFormatBadge: TextView
    private lateinit var tvCardCount: TextView
    private lateinit var tvDesc: TextView
    private lateinit var btnDiscord: Button
    private lateinit var btnAddCard: Button
    private lateinit var recyclerCards: RecyclerView
    private lateinit var cardAdapter: DeckCardAdapter

    private lateinit var editCommentInput: EditText
    private lateinit var btnSendComment: ImageButton
    private lateinit var recyclerComments: RecyclerView
    private lateinit var commentAdapter: DeckCommentAdapter

    private var currentDeck: Deck? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_details)

        setupHamburgerMenu()

        @Suppress("DEPRECATION")
        currentDeck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("deck", Deck::class.java)
        } else {
            intent.getSerializableExtra("deck") as? Deck
        }

        if (currentDeck == null) {
            Toast.makeText(this, "Deck details not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvTitle = findViewById(R.id.detailsDeckTitle)
        tvGameBadge = findViewById(R.id.detailsDeckGame)
        tvFormatBadge = findViewById(R.id.detailsDeckFormat)
        tvCardCount = findViewById(R.id.detailsDeckCardCount)
        tvDesc = findViewById(R.id.detailsDeckDesc)
        btnDiscord = findViewById(R.id.btnOpenDeckDiscord)
        btnAddCard = findViewById(R.id.btnAddCardToDeck)
        recyclerCards = findViewById(R.id.recyclerDeckCards)

        editCommentInput = findViewById(R.id.editDeckCommentInput)
        btnSendComment = findViewById(R.id.btnSendDeckComment)
        recyclerComments = findViewById(R.id.recyclerDeckComments)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
        val isOwner = currentDeck?.userId == currentUid || currentDeck?.userId == "demo_user"

        recyclerCards.layoutManager = LinearLayoutManager(this)
        cardAdapter = DeckCardAdapter(
            initial = emptyList(),
            isEditable = isOwner,
            onQuantityChange = { card, newQuantity ->
                handleQuantityChange(card, newQuantity)
            }
        )
        recyclerCards.adapter = cardAdapter

        recyclerComments.layoutManager = LinearLayoutManager(this)
        commentAdapter = DeckCommentAdapter(emptyList())
        recyclerComments.adapter = commentAdapter

        btnAddCard.visibility = if (isOwner) View.VISIBLE else View.GONE
        btnAddCard.setOnClickListener {
            openAddCardDialog()
        }

        btnSendComment.setOnClickListener {
            sendDeckComment()
        }

        bindDeckDetails()
        loadComments()
    }

    private fun bindDeckDetails() {
        val d = currentDeck ?: return

        tvTitle.text = d.deckName
        tvGameBadge.text = d.gameSystem
        tvFormatBadge.text = d.format.ifBlank { "Standard" }
        tvCardCount.text = "🃏 ${d.cards.sumOf { it.quantity }} Cards"
        tvDesc.text = d.description.ifBlank { "No strategy or deck description provided." }

        if (d.discordUrl.isNotBlank()) {
            btnDiscord.visibility = View.VISIBLE
            btnDiscord.setOnClickListener {
                openUrl(d.discordUrl)
            }
        } else {
            btnDiscord.visibility = View.GONE
        }

        cardAdapter.submitList(d.cards.toList())
    }

    private fun openUrl(rawUrl: String) {
        val formatted = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "https://$rawUrl"
        } else rawUrl
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleQuantityChange(card: DeckCard, newQuantity: Int) {
        val d = currentDeck ?: return
        val currentCards = d.cards.toMutableList()
        val idx = currentCards.indexOfFirst { it.cardName == card.cardName }

        if (idx >= 0) {
            if (newQuantity <= 0) {
                currentCards.removeAt(idx)
            } else {
                currentCards[idx] = card.copy(quantity = newQuantity)
            }
        }

        val updatedDeck = d.copy(
            cards = currentCards,
            cardCount = currentCards.sumOf { it.quantity }
        )

        currentDeck = updatedDeck
        bindDeckDetails()

        DeckManager.updateDeck(updatedDeck) { success ->
            if (!success) {
                Toast.makeText(this, "Failed to update card quantity", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAddCardDialog() {
        val d = currentDeck ?: return
        val starterCards = DeckManager.getStarterCardDatabase(d.gameSystem)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val tvHeader = TextView(this).apply {
            text = "Select card from ${d.gameSystem} database or type custom card name:"
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(tvHeader)

        val spinnerCards = Spinner(this)
        val cardNames = listOf("Custom Card...") + starterCards.map { "${it.cardName} (${it.cardType})" }
        spinnerCards.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cardNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        layout.addView(spinnerCards)

        val etCardName = EditText(this).apply {
            hint = "Card Name (e.g. Dark Magician, Sol Ring)"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        layout.addView(etCardName)

        val etQty = EditText(this).apply {
            hint = "Quantity (Default: 1)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("1")
        }
        layout.addView(etQty)

        spinnerCards.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val card = starterCards[position - 1]
                    etCardName.setText(card.cardName)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("Add Card to Deck")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val cardName = etCardName.text.toString().trim()
                val quantity = etQty.text.toString().toIntOrNull() ?: 1

                if (cardName.isEmpty()) {
                    Toast.makeText(this, "Card name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val currentCards = d.cards.toMutableList()
                val idx = currentCards.indexOfFirst { it.cardName.equals(cardName, ignoreCase = true) }

                if (idx >= 0) {
                    val existing = currentCards[idx]
                    currentCards[idx] = existing.copy(quantity = existing.quantity + quantity)
                } else {
                    val matchedStarter = starterCards.find { it.cardName.equals(cardName, ignoreCase = true) }
                    val newCard = DeckCard(
                        cardName = cardName,
                        gameSystem = d.gameSystem,
                        cardType = matchedStarter?.cardType ?: "Main",
                        quantity = quantity,
                        manaCostOrLevel = matchedStarter?.manaCostOrLevel ?: ""
                    )
                    currentCards.add(newCard)
                }

                val updatedDeck = d.copy(
                    cards = currentCards,
                    cardCount = currentCards.sumOf { it.quantity }
                )

                currentDeck = updatedDeck
                bindDeckDetails()

                DeckManager.updateDeck(updatedDeck) { success ->
                    if (success) {
                        Toast.makeText(this, "Added $cardName!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadComments() {
        val d = currentDeck ?: return
        DeckManager.getDeckComments(d.id) { comments ->
            commentAdapter.submitList(comments)
        }
    }

    private fun sendDeckComment() {
        val d = currentDeck ?: return
        val message = editCommentInput.text.toString().trim()
        if (message.isEmpty()) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
        val username = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", "Gamer") ?: "Gamer"

        val comment = DeckComment(
            deckId = d.id,
            authorUid = uid,
            authorName = username,
            message = message
        )

        editCommentInput.setText("")

        DeckManager.addDeckComment(comment) { success ->
            if (success) {
                loadComments()
            }
        }
    }
}
