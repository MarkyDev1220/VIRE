package com.vire.android.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class SearchActivity : BaseActivity() {

    private lateinit var searchView: SearchView
    private lateinit var spinnerCategory: Spinner
    private lateinit var recyclerUsers: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: UserSearchAdapter

    private val categories = listOf("All", "Username", "Favorite Game", "Local Area", "Skill Level")

    private val allSearchItems = mutableListOf<UserSearchItem>()

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setupHamburgerMenu()

        searchView = findViewById(R.id.searchViewUsers)
        spinnerCategory = findViewById(R.id.spinnerSearchCategory)
        recyclerUsers = findViewById(R.id.recyclerUsers)
        emptyText = findViewById(R.id.emptySearchText)
        progressBar = findViewById(R.id.searchProgressBar)

        recyclerUsers.layoutManager = LinearLayoutManager(this)
        adapter = UserSearchAdapter(
            onUserClick = { item ->
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("uid", item.user.id)
                startActivity(intent)
            },
            onActionClick = { item ->
                handleFriendAction(item)
            }
        )
        recyclerUsers.adapter = adapter

        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterUsers()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterUsers()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers()
                return true
            }
        })

        loadAllUsers()
    }

    private fun loadAllUsers() {
        val currentUid = auth.currentUser?.uid
        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE

        db.collection("users").get()
            .addOnSuccessListener { result ->
                allSearchItems.clear()
                val loadedUsers = mutableListOf<Pair<User, String?>>()

                for (document in result) {
                    val uid = document.id
                    if (uid == currentUid) continue

                    val name = document.getString("username")
                        ?: document.getString("fullName")
                        ?: document.getString("displayName")
                        ?: "Gamer"

                    val user = User(
                        id = uid,
                        username = name,
                        email = document.getString("email") ?: "",
                        gender = document.getString("gender") ?: "",
                        dateOfBirth = document.getString("dateOfBirth") ?: "",
                        favoriteGames = (document.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        favoriteGenres = (document.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        skillLevel = document.getString("skillLevel") ?: "",
                        localArea = document.getString("localArea") ?: "",
                        gamerBio = document.getString("gamerBio") ?: ""
                    )
                    val profileImageUrl = document.getString("profileImageUrl")
                    loadedUsers.add(Pair(user, profileImageUrl))
                }

                if (loadedUsers.isEmpty()) {
                    progressBar.visibility = View.GONE
                    filterUsers()
                    return@addOnSuccessListener
                }

                allSearchItems.clear()

                for ((user, profileImageUrl) in loadedUsers) {
                    allSearchItems.add(
                        UserSearchItem(
                            user = user,
                            profileImageUrl = profileImageUrl,
                            friendshipState = FriendManager.FriendshipState.NONE
                        )
                    )
                }

                progressBar.visibility = View.GONE
                filterUsers()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                val isFirestoreNotConfigured = e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
                        e.message?.contains("NOT_FOUND", ignoreCase = true) == true ||
                        e.message?.contains("disabled", ignoreCase = true) == true ||
                        e.message?.contains("does not exist", ignoreCase = true) == true

                val msg = if (isFirestoreNotConfigured) {
                    "Cloud Firestore database is disabled or not created in Firebase Console for project 'vire-7b583'. Showing sample gamers."
                } else {
                    "Error loading users: ${e.message}"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                Log.e("SearchActivity", "Firestore error: ${e.message}", e)
                loadDemoUsers()
            }
    }

    private fun loadDemoUsers() {
        allSearchItems.clear()
        for (user in FriendManager.getDemoUsers()) {
            allSearchItems.add(
                UserSearchItem(
                    user = user,
                    friendshipState = FriendManager.FriendshipState.NONE
                )
            )
        }
        filterUsers()
    }

    private fun filterUsers() {
        val query = searchView.query?.toString()?.trim()?.lowercase() ?: ""
        val category = spinnerCategory.selectedItem?.toString() ?: "All"

        val filtered = allSearchItems.filter { item ->
            val user = item.user
            if (query.isEmpty()) return@filter true

            val matchesUsername = user.username.lowercase().contains(query)
            val matchesEmail = user.email.lowercase().contains(query)
            val matchesArea = user.localArea.lowercase().contains(query)
            val matchesSkill = user.skillLevel.lowercase().contains(query)
            val matchesGame = user.favoriteGames.any { it.lowercase().contains(query) }
            val matchesGenre = user.favoriteGenres.any { it.lowercase().contains(query) }
            val matchesBio = user.gamerBio.lowercase().contains(query)

            when (category) {
                "Username" -> matchesUsername || matchesEmail
                "Favorite Game" -> matchesGame
                "Local Area" -> matchesArea
                "Skill Level" -> matchesSkill
                else -> matchesUsername || matchesEmail || matchesArea || matchesSkill || matchesGame || matchesGenre || matchesBio
            }
        }

        if (filtered.isEmpty() && query.length >= 2) {
            FriendManager.findUserByQuery(query) { matchedUser ->
                if (matchedUser != null && matchedUser.id != auth.currentUser?.uid) {
                    if (allSearchItems.none { it.user.id == matchedUser.id }) {
                        FriendManager.getFriendshipState(matchedUser.id) { state, reqId ->
                            val newItem = UserSearchItem(user = matchedUser, friendshipState = state, requestId = reqId)
                            allSearchItems.add(newItem)
                            adapter.submitList(listOf(newItem))
                            emptyText.visibility = View.GONE
                        }
                        return@findUserByQuery
                    }
                }
                adapter.submitList(emptyList())
                emptyText.visibility = View.VISIBLE
            }
        } else {
            adapter.submitList(filtered.toList())
            emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun handleFriendAction(item: UserSearchItem) {
        when (item.friendshipState) {
            FriendManager.FriendshipState.NONE -> {
                FriendManager.sendRequest(item.user.id) { success ->
                    if (success) {
                        Toast.makeText(this, "Friend request sent to ${item.user.username}", Toast.LENGTH_SHORT).show()
                        updateItemState(item.user.id, FriendManager.FriendshipState.PENDING_SENT, null)
                    } else {
                        Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            FriendManager.FriendshipState.PENDING_RECEIVED -> {
                val reqId = item.requestId ?: return
                FriendManager.acceptRequest(reqId) { success ->
                    if (success) {
                        Toast.makeText(this, "Accepted friend request from ${item.user.username}", Toast.LENGTH_SHORT).show()
                        updateItemState(item.user.id, FriendManager.FriendshipState.FRIENDS, null)
                    } else {
                        Toast.makeText(this, "Failed to accept request", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else -> {}
        }
    }

    private fun updateItemState(userId: String, newState: FriendManager.FriendshipState, reqId: String?) {
        val index = allSearchItems.indexOfFirst { it.user.id == userId }
        if (index >= 0) {
            val oldItem = allSearchItems[index]
            allSearchItems[index] = oldItem.copy(
                friendshipState = newState,
                requestId = reqId
            )
            filterUsers()
        }
    }
}
