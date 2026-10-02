package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
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
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class TournamentDetailsActivity : BaseActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvGameBadge: TextView
    private lateinit var tvFormatBadge: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvDateTimeLoc: TextView
    private lateinit var tvFeePrizes: TextView
    private lateinit var tvRules: TextView
    private lateinit var btnRegister: Button
    private lateinit var btnGenerateBrackets: Button
    private lateinit var emptyMatchesText: TextView
    private lateinit var recyclerMatches: RecyclerView
    private lateinit var matchAdapter: TournamentMatchAdapter

    private var currentTournament: Tournament? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_details)

        setupHamburgerMenu()

        @Suppress("DEPRECATION")
        currentTournament = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("tournament", Tournament::class.java)
        } else {
            intent.getSerializableExtra("tournament") as? Tournament
        }

        if (currentTournament == null) {
            Toast.makeText(this, "Tournament details not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvTitle = findViewById(R.id.detailsTitle)
        tvGameBadge = findViewById(R.id.detailsGameBadge)
        tvFormatBadge = findViewById(R.id.detailsFormatBadge)
        tvStatusBadge = findViewById(R.id.detailsStatusBadge)
        tvDateTimeLoc = findViewById(R.id.detailsDateTimeLoc)
        tvFeePrizes = findViewById(R.id.detailsFeePrizes)
        tvRules = findViewById(R.id.detailsRules)
        btnRegister = findViewById(R.id.btnRegisterTournament)
        btnGenerateBrackets = findViewById(R.id.btnGenerateBrackets)
        emptyMatchesText = findViewById(R.id.emptyMatchesText)
        recyclerMatches = findViewById(R.id.recyclerMatches)

        recyclerMatches.layoutManager = LinearLayoutManager(this)
        matchAdapter = TournamentMatchAdapter(emptyList()) { match ->
            openScoreReportDialog(match)
        }
        recyclerMatches.adapter = matchAdapter

        val btnDiscord = findViewById<Button>(R.id.btnOpenDiscord)
        val btnExternalBracket = findViewById<Button>(R.id.btnOpenExternalBracket)

        btnRegister.setOnClickListener {
            handleRegisterAction()
        }

        btnGenerateBrackets.setOnClickListener {
            handleGenerateBrackets()
        }

        val t = currentTournament
        if (t != null && t.discordUrl.isNotBlank()) {
            btnDiscord.visibility = View.VISIBLE
            btnDiscord.setOnClickListener {
                openUrl(t.discordUrl)
            }
        }

        if (t != null && t.externalBracketUrl.isNotBlank()) {
            btnExternalBracket.visibility = View.VISIBLE
            btnExternalBracket.setOnClickListener {
                openUrl(t.externalBracketUrl)
            }
        }

        bindTournamentDetails()
        loadMatches()
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

    private fun bindTournamentDetails() {
        val t = currentTournament ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        tvTitle.text = t.name
        tvGameBadge.text = t.gameSystem
        tvFormatBadge.text = t.format
        tvStatusBadge.text = t.status

        val dateLocStr = listOf(t.date, t.time, t.location)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
        tvDateTimeLoc.text = "📅 ${dateLocStr.ifBlank { "Schedule TBD" }}"

        tvFeePrizes.text = "💵 Fee: ${t.entryFee.ifBlank { "Free" }} • Host: ${t.organizer} • Players: ${t.participants.size}/${t.maxParticipants}"

        var rulesText = ""
        if (t.rules.isNotBlank()) rulesText += "Rules:\n${t.rules}\n\n"
        if (t.prizesDescription.isNotBlank()) rulesText += "Prizes:\n${t.prizesDescription}"
        tvRules.text = rulesText.ifBlank { "No special rules or prizes description provided." }

        // Registration button state
        val isRegistered = t.participants.contains(currentUid)
        if (isRegistered) {
            btnRegister.text = "Unregister / Cancel Entry"
            btnRegister.setBackgroundColor(Color.parseColor("#C62828")) // Red
        } else {
            btnRegister.text = "Register for Event"
            btnRegister.setBackgroundColor(Color.parseColor("#1565C0")) // Blue
        }

        // Host controls
        val isHost = t.hostUid == currentUid || t.organizer.equals("admin", true)
        if (isHost && t.status == "Upcoming") {
            btnGenerateBrackets.visibility = View.VISIBLE
        } else {
            btnGenerateBrackets.visibility = View.GONE
        }
    }

    private fun loadMatches() {
        val t = currentTournament ?: return
        TournamentManager.getMatches(t.id) { matches ->
            matchAdapter.submitList(matches)
            emptyMatchesText.visibility = if (matches.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun handleRegisterAction() {
        val t = currentTournament ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

        val isRegistered = t.participants.contains(currentUid)
        if (isRegistered) {
            TournamentManager.unregisterPlayer(t.id, currentUid) { success ->
                if (success) {
                    Toast.makeText(this, "Unregistered from event", Toast.LENGTH_SHORT).show()
                    val newParticipants = t.participants.filter { it != currentUid }
                    currentTournament = t.copy(participants = newParticipants)
                    bindTournamentDetails()
                }
            }
        } else {
            if (t.participants.size >= t.maxParticipants) {
                Toast.makeText(this, "Event is full!", Toast.LENGTH_SHORT).show()
                return
            }

            TournamentManager.registerPlayer(t.id, currentUid) { success ->
                if (success) {
                    Toast.makeText(this, "Successfully registered!", Toast.LENGTH_SHORT).show()
                    val newParticipants = t.participants + currentUid
                    currentTournament = t.copy(participants = newParticipants)
                    bindTournamentDetails()
                }
            }
        }
    }

    private fun handleGenerateBrackets() {
        val t = currentTournament ?: return
        if (t.participants.isEmpty()) {
            Toast.makeText(this, "No registered players to pair!", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch player usernames for bracket display
        FirebaseFirestore.getInstance().collection("users").get()
            .addOnSuccessListener { result ->
                val nameMap = mutableMapOf<String, String>()
                for (doc in result) {
                    val username = doc.getString("username") ?: "Gamer"
                    nameMap[doc.id] = username
                }

                TournamentManager.generateBrackets(t, nameMap) { success ->
                    if (success) {
                        Toast.makeText(this, "Brackets & Round 1 paired!", Toast.LENGTH_SHORT).show()
                        currentTournament = t.copy(status = "In Progress")
                        bindTournamentDetails()
                        loadMatches()
                    } else {
                        Toast.makeText(this, "Failed to generate brackets", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                TournamentManager.generateBrackets(t, emptyMap()) { success ->
                    if (success) {
                        currentTournament = t.copy(status = "In Progress")
                        bindTournamentDetails()
                        loadMatches()
                    }
                }
            }
    }

    private fun openScoreReportDialog(match: TournamentMatch) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val tvTitle = TextView(this).apply {
            text = "Enter match scores for ${match.player1Name} vs ${match.player2Name}"
            textSize = 14f
        }
        layout.addView(tvTitle)

        val etScore1 = EditText(this).apply {
            hint = "${match.player1Name} Score (e.g. 2)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(match.player1Score.toString())
        }
        layout.addView(etScore1)

        val etScore2 = EditText(this).apply {
            hint = "${match.player2Name} Score (e.g. 1)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(match.player2Score.toString())
        }
        layout.addView(etScore2)

        val rgWinner = RadioGroup(this)
        val rbP1 = RadioButton(this).apply {
            id = View.generateViewId()
            text = "Winner: ${match.player1Name}"
        }
        val rbP2 = RadioButton(this).apply {
            id = View.generateViewId()
            text = "Winner: ${match.player2Name}"
        }
        rgWinner.addView(rbP1)
        rgWinner.addView(rbP2)
        if (match.winnerUid == match.player1Uid) rbP1.isChecked = true
        else if (match.winnerUid == match.player2Uid) rbP2.isChecked = true
        else rbP1.isChecked = true

        layout.addView(rgWinner)

        AlertDialog.Builder(this)
            .setTitle("Report Match Result")
            .setView(layout)
            .setPositiveButton("Submit Result") { _, _ ->
                val s1 = etScore1.text.toString().toIntOrNull() ?: 0
                val s2 = etScore2.text.toString().toIntOrNull() ?: 0
                val winnerUid = if (rbP1.isChecked) match.player1Uid else match.player2Uid

                TournamentManager.updateMatchScore(match, s1, s2, winnerUid) { success ->
                    if (success) {
                        Toast.makeText(this, "Match result recorded!", Toast.LENGTH_SHORT).show()
                        loadMatches()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
