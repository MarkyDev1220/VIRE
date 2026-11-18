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

        setupStandardBottomNav(BottomTab.COMMUNITIES)

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
                val loadedUsers = mutableListOf<UserSearchItem>()

                for (document in result) {
                    val uid = document.id
                    if (uid == currentUid) continue

                    val email = document.getString("email")
                        ?: document.getString("emailAddress")
                        ?: ""

                    val rawUsername = document.getString("username")
                        ?: document.getString("displayName")
                        ?: document.getString("name")
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val fullName = document.getString("fullName")
                        ?.takeIf { it.isNotBlank() }
                        ?: "$firstName $lastName".trim()
                    val username = when {
                        !rawUsername.isNullOrBlank() -> rawUsername
                        email.isNotBlank() -> email.substringBefore("@")
                        fullName.isNotBlank() -> fullName
                        else -> "User (${uid.take(6)})"
                    }

                    if (!document.contains("usernameLowercase") && rawUsername?.isNotBlank() == true) {
                        db.collection("users").document(uid)
                            .update("usernameLowercase", rawUsername.lowercase())
                    }

                    val user = User(
                        id = uid,
                        username = username,
                        email = email,
                        firstName = firstName,
                        lastName = lastName,
                        fullName = fullName,
                        gender = document.getString("gender") ?: "",
                        dateOfBirth = document.getString("dateOfBirth") ?: "",
                        favoriteGames = (document.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        favoriteGenres = (document.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        skillLevel = document.getString("skillLevel") ?: "",
                        localArea = document.getString("localArea") ?: "",
                        gamerBio = document.getString("gamerBio") ?: "",
                        aboutMe = document.getString("aboutMe") ?: ""
                    )
                    val profileImageUrl = document.getString("profileImageUrl")
                        ?: document.getString("profileImage")
                        ?: document.getString("avatarUrl")
                        ?: document.getString("profileImageUri")
                    val coverImageUrl = document.getString("coverImageUrl")
                        ?: document.getString("coverImage")
                        ?: document.getString("coverUrl")
                        ?: document.getString("coverImageUri")
                    loadedUsers.add(
                        UserSearchItem(
                            user = user,
                            profileImageUrl = profileImageUrl,
                            coverImageUrl = coverImageUrl,
                            profileTheme = document.getString("profileTheme")
                        )
                    )
                }

                allSearchItems.clear()
                allSearchItems.addAll(loadedUsers)

                progressBar.visibility = View.GONE
                filterUsers()

                if (currentUid != null) {
                    for (item in loadedUsers) {
                        val user = item.user
                        FriendManager.getFriendshipState(user.id) { state, reqId ->
                                val index = allSearchItems.indexOfFirst { it.user.id == user.id }
                                if (index != -1) {
                                    allSearchItems[index] = allSearchItems[index].copy(
                                        friendshipState = state,
                                        requestId = reqId
                                    )
                                    filterUsers()
                                }
                            }
                    }
                }

                loadedUsers.forEach { item ->
                    val storage = try {
                        com.google.firebase.storage.FirebaseStorage.getInstance().reference
                    } catch (_: IllegalStateException) {
                        null
                    } ?: return@forEach

                    if (!isRemoteImageValue(item.profileImageUrl)) {
                        storage.child("profileImages/${item.user.id}/profileImageUrl.jpg")
                            .downloadUrl
                            .addOnSuccessListener { uri ->
                                updateSearchItem(item.user.id) { current ->
                                    current.copy(profileImageUrl = uri.toString())
                                }
                            }
                    }
                    if (!isRemoteImageValue(item.coverImageUrl)) {
                        storage.child("profileImages/${item.user.id}/coverImageUrl.jpg")
                            .downloadUrl
                            .addOnSuccessListener { uri ->
                                updateSearchItem(item.user.id) { current ->
                                    current.copy(coverImageUrl = uri.toString())
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading users: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("SearchActivity", "Firestore error: ${e.message}", e)
                allSearchItems.clear()
                filterUsers()
            }
    }

    private fun filterUsers() {
        val query = searchView.query?.toString()?.trim()?.lowercase() ?: ""
        val category = spinnerCategory.selectedItem?.toString() ?: "All"

        val filtered = allSearchItems.filter { item ->
            val user = item.user
            if (query.isEmpty()) return@filter true

            val matchesUsername = user.username.lowercase().contains(query) ||
                    user.fullName.lowercase().contains(query)
            val matchesEmail = user.email.lowercase().contains(query)
            val matchesArea = user.localArea.lowercase().contains(query)
            val matchesSkill = user.skillLevel.lowercase().contains(query)
            val matchesGame = user.favoriteGames.any { it.lowercase().contains(query) }
            val matchesGenre = user.favoriteGenres.any { it.lowercase().contains(query) }
            val matchesBio = user.gamerBio.lowercase().contains(query) ||
                    user.aboutMe.lowercase().contains(query)

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

    private fun isRemoteImageValue(value: String?): Boolean =
        value?.let {
            it.startsWith("https://", ignoreCase = true) ||
                it.startsWith("http://", ignoreCase = true) ||
                it.startsWith("data:image", ignoreCase = true) ||
                it.startsWith("gs://", ignoreCase = true)
        } == true

    private fun updateSearchItem(userId: String, transform: (UserSearchItem) -> UserSearchItem) {
        val index = allSearchItems.indexOfFirst { it.user.id == userId }
        if (index >= 0) {
            allSearchItems[index] = transform(allSearchItems[index])
            filterUsers()
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
