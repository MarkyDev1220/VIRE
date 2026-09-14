package com.vire.android.android

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class SearchActivity : BaseActivity() {

    private lateinit var searchInput: EditText
    private lateinit var usersListView: ListView
    private lateinit var usersAdapter: ArrayAdapter<String>
    private val displayedUsers = mutableListOf<User>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setupHamburgerMenu()

        searchInput = findViewById(R.id.searchInput)
        usersListView = findViewById(R.id.usersListView)

        usersAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        usersListView.adapter = usersAdapter

        loadAllUsers()

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Tap: view profile
        usersListView.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = displayedUsers[position]
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("uid", selectedUser.id)
            startActivity(intent)
        }

        // Long press: add friend
        usersListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedUser = displayedUsers[position]
            val currentUid = auth.currentUser?.uid

            if (currentUid == null || selectedUser.id == currentUid) return@setOnItemLongClickListener true

            AlertDialog.Builder(this)
                .setTitle("Add Friend")
                .setMessage("Do you want to send a friend request to ${selectedUser.username}?")
                .setPositiveButton("Yes") { _, _ ->
                    FriendManager.sendRequest(selectedUser.id) { success ->
                        Toast.makeText(
                            this@SearchActivity,
                            if (success) "Friend request sent" else "Failed to send request",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("No", null)
                .show()

            true
        }
    }

    private fun loadAllUsers() {
        val currentUid = auth.currentUser?.uid
        db.collection("users").get()
            .addOnSuccessListener { result ->
                displayedUsers.clear()
                for (document in result) {
                    val uid = document.id
                    if (uid == currentUid) continue

                    val user = User(
                        id = uid,
                        username = document.getString("username") ?: "Unknown",
                        email = document.getString("email") ?: "",
                        gender = document.getString("gender") ?: "",
                        dateOfBirth = document.getString("dateOfBirth") ?: "",
                        favoriteGames = (document.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        favoriteGenres = (document.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        skillLevel = document.getString("skillLevel") ?: "",
                        localArea = document.getString("localArea") ?: "",
                        gamerBio = document.getString("gamerBio") ?: ""
                    )
                    displayedUsers.add(user)
                }
                updateUserDisplay()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading users: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("SearchActivity", "Firestore error", e)
            }
    }

    private fun filterUsers(query: String) {
        val filtered = displayedUsers.filter { it.username.contains(query, ignoreCase = true) }
        updateUserDisplay(filtered)
    }

    private fun updateUserDisplay(users: List<User> = displayedUsers) {
        val names = users.map { it.username }
        usersAdapter.clear()
        if (names.isEmpty()) {
            // Optional: You could show a "No users found" label here
            usersAdapter.add("No users found")
        } else {
            usersAdapter.addAll(names)
        }
        usersAdapter.notifyDataSetChanged()
    }
}

