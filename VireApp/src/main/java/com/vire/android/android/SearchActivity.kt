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
    private var blockedUserIds: Set<String> = emptySet()

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
        if (currentUid == null) {
            progressBar.visibility = View.GONE
            allSearchItems.clear()
            adapter.submitList(emptyList())
            emptyText.text = "Sign in to search for users."
            emptyText.visibility = View.VISIBLE
            return
        }

        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE

        db.collection(UserDirectory.COLLECTION).get()
            .addOnSuccessListener { result ->
                allSearchItems.clear()
                val loadedUsers = mutableListOf<Pair<User, String?>>()

                for (document in result) {
                    val uid = document.id

                    val email = document.getString("email")
                        ?: document.getString("emailAddress")
                        ?: ""

                    val rawName = document.getString("username")
                        ?: document.getString("fullName")
                        ?: document.getString("displayName")
                        ?: document.getString("name")

                    val displayName = when {
                        !rawName.isNullOrBlank() -> rawName
                        email.isNotBlank() -> email.substringBefore("@")
                        else -> "User (${uid.take(6)})"
                    } + if (uid == currentUid) " (You)" else ""

                    val user = User(
                        id = uid,
                        username = displayName,
                        email = email,
                        gender = document.getString("gender") ?: "",
                        dateOfBirth = document.getString("dateOfBirth") ?: "",
                        favoriteGames = (document.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        favoriteGenres = (document.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        skillLevel = document.getString("skillLevel") ?: "",
                        localArea = document.getString("localArea") ?: "",
                        gamerBio = document.getString("gamerBio") ?: ""
                    )
                    val profileImageUrl = document.getString("profileImageUrl")
                        ?: document.getString("profileImage")
                        ?: document.getString("avatarUrl")
                        ?: document.getString("profileImageUri")
                    loadedUsers.add(Pair(user, profileImageUrl))
                }

                if (loadedUsers.isEmpty()) {
                    progressBar.visibility = View.GONE
                    filterUsers()
                    return@addOnSuccessListener
                }

                UserManager.setUsers(loadedUsers.map { it.first })

                TrustSafetyManager.getBlockedUserIds { blocked ->
                    blockedUserIds = blocked
                    FriendManager.getFriends { friendUids ->
                        FriendManager.getPendingRequests { pendingReqs ->
                            val pendingReqIdMap = pendingReqs.associate { it.second to it.first }

                            db.collection("friendRequests")
                                .whereEqualTo("fromUid", currentUid)
                                .whereEqualTo("status", "pending")
                                .get()
                                .addOnSuccessListener { sentSnap ->
                                    val sentReqIdMap = sentSnap.documents.mapNotNull { doc ->
                                        val to = doc.getString("toUid") ?: return@mapNotNull null
                                        to to doc.id
                                    }.toMap()

                                    populateSearchItems(loadedUsers, friendUids, pendingReqIdMap, sentReqIdMap, currentUid)
                                }
                                .addOnFailureListener {
                                    populateSearchItems(loadedUsers, friendUids, pendingReqIdMap, emptyMap(), null)
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                val isPermissionDenied =
                    e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                val isDatabaseConfigurationError =
                    e.message?.contains("NOT_FOUND", ignoreCase = true) == true ||
                        e.message?.contains("disabled", ignoreCase = true) == true ||
                        e.message?.contains("does not exist", ignoreCase = true) == true

                Log.w("SearchActivity", "Firestore error loading users: ${e.message}", e)
                val errorMessage = when {
                    isPermissionDenied ->
                        "Firestore denied access to public profiles. Check the signed-in account and public profile rules."
                    isDatabaseConfigurationError ->
                        "The Firestore database is unavailable. Check the Firebase project configuration."
                    else -> "Error loading users: ${e.localizedMessage}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                allSearchItems.clear()
                adapter.submitList(emptyList())
                emptyText.text = "Users could not be loaded. Please try again."
                emptyText.visibility = View.VISIBLE
            }
    }

    private fun populateSearchItems(
        loadedUsers: List<Pair<User, String?>>,
        friendUids: List<String>,
        pendingReqIdMap: Map<String, String>,
        sentReqIdMap: Map<String, String>,
        currentUid: String?
    ) {
        allSearchItems.clear()
        for ((user, profileImageUrl) in loadedUsers) {
            if (user.id !in blockedUserIds) {
                val state = when {
                    currentUid != null && user.id == currentUid -> FriendManager.FriendshipState.NONE
                    friendUids.contains(user.id) -> FriendManager.FriendshipState.FRIENDS
                    sentReqIdMap.containsKey(user.id) -> FriendManager.FriendshipState.PENDING_SENT
                    pendingReqIdMap.containsKey(user.id) -> FriendManager.FriendshipState.PENDING_RECEIVED
                    else -> FriendManager.FriendshipState.NONE
                }
                val reqId = when (state) {
                    FriendManager.FriendshipState.PENDING_SENT -> sentReqIdMap[user.id]
                    FriendManager.FriendshipState.PENDING_RECEIVED -> pendingReqIdMap[user.id]
                    else -> null
                }
                allSearchItems.add(
                    UserSearchItem(
                        user = user,
                        profileImageUrl = profileImageUrl,
                        friendshipState = state,
                        requestId = reqId
                    )
                )
            }
        }
        progressBar.visibility = View.GONE
        filterUsers()
    }

    private fun filterUsers() {
        val query = searchView.query?.toString()?.trim()?.lowercase() ?: ""
        val category = spinnerCategory.selectedItem?.toString() ?: "All"

        val filtered = allSearchItems.filter { item ->
            val user = item.user
            if (user.id in blockedUserIds) return@filter false
            if (query.isEmpty()) return@filter true

            val matchesUsername = user.username.lowercase().contains(query)
            val matchesArea = user.localArea.lowercase().contains(query)
            val matchesSkill = user.skillLevel.lowercase().contains(query)
            val matchesGame = user.favoriteGames.any { it.lowercase().contains(query) }
            val matchesGenre = user.favoriteGenres.any { it.lowercase().contains(query) }
            val matchesBio = user.gamerBio.lowercase().contains(query)

            when (category) {
                "Username" -> matchesUsername
                "Favorite Game" -> matchesGame
                "Local Area" -> matchesArea
                "Skill Level" -> matchesSkill
                else -> matchesUsername || matchesArea || matchesSkill || matchesGame || matchesGenre || matchesBio
            }
        }

        if (filtered.isEmpty() && query.length >= 2) {
            FriendManager.findUserByQuery(query) { matchedUser ->
                if (matchedUser != null && matchedUser.id != auth.currentUser?.uid && matchedUser.id !in blockedUserIds) {
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
