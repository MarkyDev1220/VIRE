package com.vire.android.android

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class SearchActivity : BaseActivity() {

    private lateinit var searchInput: EditText
    private lateinit var usersListView: ListView
    private lateinit var usersAdapter: ArrayAdapter<String>
    private val displayedUsers = mutableListOf<User>()

    private val loggedInUserId = 1L // Replace with actual logged-in user ID
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Initialize Hamburger Menu from BaseActivity
        setupHamburgerMenu()

        searchInput = findViewById(R.id.searchInput)
        usersListView = findViewById(R.id.usersListView)

        usersAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        usersListView.adapter = usersAdapter

        // Load all users from Firestore
        loadAllUsers()

        // Search input listener
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Click: view profile
        usersListView.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = displayedUsers[position]
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("uid", selectedUser.id.toString())
            intent.putExtra("username", selectedUser.username)
            startActivity(intent)
        }

        // Long click: add/remove friend
        usersListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedUser = displayedUsers[position]

            if (selectedUser.id != loggedInUserId) {
                if (!selectedUser.friends.contains(loggedInUserId)) {
                    AlertDialog.Builder(this)
                        .setTitle("Add Friend")
                        .setMessage("Do you want to add ${selectedUser.username} as a friend?")
                        .setPositiveButton("Yes") { _, _ ->
                            Toast.makeText(this, "${selectedUser.username} added as a friend", Toast.LENGTH_SHORT).show()
                            loadAllUsers()
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Remove Friend")
                        .setMessage("Do you want to remove ${selectedUser.username} from friends?")
                        .setPositiveButton("Yes") { _, _ ->
                            Toast.makeText(this, "${selectedUser.username} removed from friends", Toast.LENGTH_SHORT).show()
                            loadAllUsers()
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
            true
        }
    }

    private fun loadAllUsers() {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                displayedUsers.clear()
                for (document in result) {
                    val user = User(
                        id = document.id.toLongOrNull() ?: 0L,
                        username = document.getString("username") ?: "Unknown",
                        email = document.getString("email") ?: "",
                        gender = document.getString("gender") ?: "",
                        dateOfBirth = document.getString("dateOfBirth") ?: "",
                        favoriteGames = (document.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                    if (user.id != loggedInUserId) {
                        displayedUsers.add(user)
                    }
                }
                updateUserDisplay()
            }
    }

    private fun filterUsers(query: String) {
        val filtered = displayedUsers.filter { it.username.contains(query, ignoreCase = true) }
        updateUserDisplay(filtered)
    }

    private fun updateUserDisplay(users: List<User> = displayedUsers) {
        val names = users.map { it.username }
        usersAdapter.clear()
        usersAdapter.addAll(names)
        usersAdapter.notifyDataSetChanged()
    }
}
