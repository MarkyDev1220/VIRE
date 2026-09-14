package com.vire.android.android

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class PlayerFinderActivity : BaseActivity() {

    private lateinit var gameInput: EditText
    private lateinit var areaInput: EditText
    private lateinit var skillInput: EditText
    private lateinit var playersListView: ListView
    private lateinit var playersAdapter: ArrayAdapter<String>

    private val displayedPlayers = mutableListOf<User>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_finder)

        setupHamburgerMenu()

        gameInput = findViewById(R.id.gameFilterInput)
        areaInput = findViewById(R.id.areaFilterInput)
        skillInput = findViewById(R.id.skillFilterInput)
        playersListView = findViewById(R.id.playersListView)

        playersAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        playersListView.adapter = playersAdapter

        loadPlayers()

        val filterWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        gameInput.addTextChangedListener(filterWatcher)
        areaInput.addTextChangedListener(filterWatcher)
        skillInput.addTextChangedListener(filterWatcher)

        playersListView.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = displayedPlayers[position]
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("uid", selectedUser.id)
            startActivity(intent)
        }

        playersListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedUser = displayedPlayers[position]
            val currentUid = auth.currentUser?.uid

            if (currentUid == null || selectedUser.id == currentUid) return@setOnItemLongClickListener true

            FriendManager.isFriend(selectedUser.id) { alreadyFriends ->
                if (alreadyFriends) {
                    Toast.makeText(this, "${selectedUser.username} is already your friend.", Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Add Friend")
                        .setMessage("Send friend request to ${selectedUser.username}?")
                        .setPositiveButton("Yes") { _, _ ->
                            FriendManager.sendRequest(selectedUser.id) { success ->
                                Toast.makeText(
                                    this@PlayerFinderActivity,
                                    if (success) "Friend request sent" else "Failed to send request (already pending?)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }

            true
        }
    }

    private fun loadPlayers() {
        val currentUid = auth.currentUser?.uid
        db.collection("users").get()
            .addOnSuccessListener { result ->
                displayedPlayers.clear()
                for (doc in result) {
                    val uid = doc.id
                    if (uid == currentUid) continue

                    val user = User(
                        id = uid,
                        username = doc.getString("username") ?: "Unknown",
                        email = doc.getString("email") ?: "",
                        gender = doc.getString("gender") ?: "",
                        dateOfBirth = doc.getString("dateOfBirth") ?: "",
                        favoriteGames = (doc.get("favoriteGames") as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList(),
                        favoriteGenres = (doc.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList(),
                        skillLevel = doc.getString("skillLevel") ?: "",
                        localArea = doc.getString("localArea") ?: "",
                        gamerBio = doc.getString("gamerBio") ?: ""
                    )
                    displayedPlayers.add(user)
                }
                updatePlayerDisplay(displayedPlayers)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error finding players: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun applyFilters() {
        val gameQuery = gameInput.text.toString().trim()
        val areaQuery = areaInput.text.toString().trim()
        val skillQuery = skillInput.text.toString().trim()

        val filtered = displayedPlayers.filter { user ->
            val gameMatch = if (gameQuery.isNotEmpty()) {
                user.favoriteGames.any { it.contains(gameQuery, ignoreCase = true) }
            } else true

            val areaMatch = if (areaQuery.isNotEmpty()) {
                user.localArea.contains(areaQuery, ignoreCase = true)
            } else true

            val skillMatch = if (skillQuery.isNotEmpty()) {
                user.skillLevel.contains(skillQuery, ignoreCase = true)
            } else true

            gameMatch && areaMatch && skillMatch
        }

        updatePlayerDisplay(filtered)
    }

    private fun updatePlayerDisplay(users: List<User>) {
        val labels = users.map {
            "${it.username} • ${it.skillLevel.ifEmpty { "Skill N/A" }} • ${it.localArea.ifEmpty { "Area N/A" }}"
        }
        playersAdapter.clear()
        playersAdapter.addAll(labels)
        playersAdapter.notifyDataSetChanged()
    }
}

