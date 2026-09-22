package com.vire.android.android

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class FriendsActivity : BaseActivity() {

    private lateinit var searchInput: EditText
    private lateinit var friendsListView: ListView
    private lateinit var friendsAdapter: ArrayAdapter<String>
    private val displayedFriends = mutableListOf<User>()

    private lateinit var requestsListView: ListView
    private lateinit var requestsAdapter: ArrayAdapter<String>
    private val displayedRequests = mutableListOf<Pair<String, String>>() // (requestId, fromUid)

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        searchInput = findViewById(R.id.searchInputFriends)
        friendsListView = findViewById(R.id.friendsListView)
        requestsListView = findViewById(R.id.requestsListView)

        friendsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        friendsListView.adapter = friendsAdapter

        requestsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        requestsListView.adapter = requestsAdapter

        loadFriends()
        loadFriendRequests()

        friendsListView.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = displayedFriends[position]
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("uid", selectedUser.id)
            startActivity(intent)
        }

        friendsListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedUser = displayedFriends[position]
            AlertDialog.Builder(this)
                .setTitle("Remove Friend")
                .setMessage("Do you want to remove ${selectedUser.username} from your friends?")
                .setPositiveButton("Yes") { _, _ ->
                    FriendManager.removeFriend(selectedUser.id) { success ->
                        Toast.makeText(
                            this,
                            if (success) "Friend removed" else "Failed to remove friend",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadFriends()
                    }
                }
                .setNegativeButton("No", null)
                .show()
            true
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFriends(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton?.setOnClickListener { showMenu(it) }
    }

    private fun loadFriends() {
        FriendManager.getFriends { friendUids ->
            if (friendUids.isEmpty()) {
                displayedFriends.clear()
                updateFriendList(displayedFriends)
                return@getFriends
            }

            db.collection("users")
                .whereIn("__name__", friendUids)
                .get()
                .addOnSuccessListener { result ->
                    displayedFriends.clear()
                    for (doc in result) {
                        val uid = doc.id
                        val user = User(
                            id = uid,
                            username = doc.getString("username") ?: "Unknown",
                            email = doc.getString("email") ?: "",
                            gender = doc.getString("gender") ?: "",
                            dateOfBirth = doc.getString("dateOfBirth") ?: "",
                            favoriteGames = (doc.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            favoriteGenres = (doc.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            skillLevel = doc.getString("skillLevel") ?: "",
                            localArea = doc.getString("localArea") ?: "",
                            gamerBio = doc.getString("gamerBio") ?: ""
                        )
                        displayedFriends.add(user)
                    }
                    updateFriendList(displayedFriends)
                }
        }
    }

    private fun loadFriendRequests() {
        FriendManager.getPendingRequests { requests ->
            displayedRequests.clear()
            displayedRequests.addAll(requests)

            if (requests.isEmpty()) {
                requestsAdapter.clear()
                requestsAdapter.notifyDataSetChanged()
                return@getPendingRequests
            }

            val fromUids = requests.map { it.second }
            db.collection("users")
                .whereIn("__name__", fromUids)
                .get()
                .addOnSuccessListener { result ->
                    val nameMap = result.documents.associateBy({ it.id }) {
                        it.getString("username") ?: "Unknown"
                    }
                    val labels = mutableListOf<String>()
                    for ((_, fromUid) in requests) {
                        val name = nameMap[fromUid] ?: fromUid
                        labels.add("Request from $name")
                    }
                    requestsAdapter.clear()
                    requestsAdapter.addAll(labels)
                    requestsAdapter.notifyDataSetChanged()
                }
        }

        requestsListView.setOnItemClickListener { _, _, position, _ ->
            val (reqId, fromUid) = displayedRequests[position]
            AlertDialog.Builder(this)
                .setTitle("Friend Request")
                .setMessage("Accept friend request from $fromUid?")
                .setPositiveButton("Accept") { _, _ ->
                    FriendManager.acceptRequest(reqId) { success ->
                        Toast.makeText(
                            this,
                            if (success) "Friend request accepted" else "Failed to accept",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadFriends()
                        loadFriendRequests()
                    }
                }
                .setNegativeButton("Decline") { _, _ ->
                    FriendManager.declineRequest(reqId) { success ->
                        Toast.makeText(
                            this,
                            if (success) "Request declined" else "Failed to decline",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadFriendRequests()
                    }
                }
                .show()
        }
    }

    private fun updateFriendList(friends: List<User>) {
        val names = friends.map { it.username }
        friendsAdapter.clear()
        friendsAdapter.addAll(names)
        friendsAdapter.notifyDataSetChanged()
    }

    private fun filterFriends(query: String) {
        val filtered = displayedFriends.filter { it.username.contains(query, ignoreCase = true) }
        updateFriendList(filtered)
    }

    private fun showMenu(view: View) {
        val popup = PopupMenu(this, view)
        val menuItems = listOf(
            "Home", "Profile", "Messages", "Buy/Sell", "Challenges",
            "Quest", "Settings", "Tournaments", "Rankings", "Friends", "Search", "Find Players"
        )
        menuItems.forEach { popup.menu.add(it) }

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
                "Find Players" -> startActivity(Intent(this, PlayerFinderActivity::class.java))
            }
            true
        }
        popup.show()
    }
}
