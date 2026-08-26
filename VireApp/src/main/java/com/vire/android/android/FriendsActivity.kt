package com.vire.android.android

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.vire.android.R

class FriendsActivity : BaseActivity() {

    private lateinit var searchInput: EditText
    private lateinit var friendsListView: ListView
    private lateinit var friendsAdapter: ArrayAdapter<String>
    private val displayedFriends = mutableListOf<User>()

    private val loggedInUserId = 1L // Replace with actual logged-in user ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        searchInput = findViewById(R.id.searchInputFriends)
        friendsListView = findViewById(R.id.friendsListView)

        friendsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        friendsListView.adapter = friendsAdapter

        loadFriends()

        // Click to view friend's profile
        friendsListView.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = displayedFriends[position]
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("username", selectedUser.username)
            startActivity(intent)
        }

        // Long click to remove friend
        friendsListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedUser = displayedFriends[position]
            AlertDialog.Builder(this)
                .setTitle("Remove Friend")
                .setMessage("Do you want to remove ${selectedUser.username} from your friends?")
                .setPositiveButton("Yes") { _, _ ->
                    UserManager.removeFriend(loggedInUserId, selectedUser.id)
                    Toast.makeText(this, "${selectedUser.username} removed from friends", Toast.LENGTH_SHORT).show()
                    loadFriends()
                }
                .setNegativeButton("No", null)
                .show()
            true
        }

        // Search bar filtering
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFriends(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Hamburger menu
        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener { showMenu(it) }
    }

    private fun loadFriends() {
        val friends = UserManager.getFriends(loggedInUserId)
        updateFriendList(friends)
    }

    private fun updateFriendList(friends: List<User>) {
        displayedFriends.clear()
        displayedFriends.addAll(friends)

        val names = displayedFriends.map { it.username }
        friendsAdapter.clear()
        friendsAdapter.addAll(names)
        friendsAdapter.notifyDataSetChanged()
    }

    private fun filterFriends(query: String) {
        val filtered = displayedFriends.filter { it.username.contains(query, ignoreCase = true) }
        friendsAdapter.clear()
        friendsAdapter.addAll(filtered.map { it.username })
        friendsAdapter.notifyDataSetChanged()
    }

    private fun showMenu(view: android.view.View) {
        val popup = android.widget.PopupMenu(this, view)
        val menuItems = listOf(
            "Home", "Profile", "Messages", "Buy/Sell", "Challenges",
            "Quest", "Settings", "Tournaments", "Rankings", "Friends", "Search"
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
            }
            true
        }
        popup.show()
    }
}
