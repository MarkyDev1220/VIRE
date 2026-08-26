package com.vire.android.android

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.vire.android.R

class SearchActivity : BaseActivity() {

    private lateinit var searchInput: EditText
    private lateinit var usersListView: ListView
    private lateinit var usersAdapter: ArrayAdapter<String>
    private val displayedUsers = mutableListOf<User>()

    private val loggedInUserId = 1L // Replace with actual logged-in user ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchInput = findViewById(R.id.searchInput)
        usersListView = findViewById(R.id.usersListView)

        usersAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        usersListView.adapter = usersAdapter

        // Load all users except the logged-in user
        updateUserList(UserManager.getAllUserObjects().filter { it.id != loggedInUserId })

        // Search input listener
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val results = UserManager.searchUsers(s.toString(), excludeUserId = loggedInUserId)
                updateUserList(results)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Click: view profile
        usersListView.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = displayedUsers[position]
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("username", selectedUser.username)
            startActivity(intent)
        }

        // Long click: add/remove friend
        usersListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedUser = displayedUsers[position]
            val currentUser = UserManager.getUserById(loggedInUserId)

            if (currentUser != null && selectedUser.id != loggedInUserId) {
                if (!currentUser.friends.contains(selectedUser.id)) {
                    AlertDialog.Builder(this)
                        .setTitle("Add Friend")
                        .setMessage("Do you want to add ${selectedUser.username} as a friend?")
                        .setPositiveButton("Yes") { _, _ ->
                            UserManager.addFriend(loggedInUserId, selectedUser.id)
                            Toast.makeText(this, "${selectedUser.username} added as a friend", Toast.LENGTH_SHORT).show()
                            updateUserList(UserManager.getAllUserObjects().filter { it.id != loggedInUserId })
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Remove Friend")
                        .setMessage("Do you want to remove ${selectedUser.username} from friends?")
                        .setPositiveButton("Yes") { _, _ ->
                            UserManager.removeFriend(loggedInUserId, selectedUser.id)
                            Toast.makeText(this, "${selectedUser.username} removed from friends", Toast.LENGTH_SHORT).show()
                            updateUserList(UserManager.getAllUserObjects().filter { it.id != loggedInUserId })
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
            true
        }

        // Hamburger menu (bottom-right)
        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener { showMenu(it) }
    }

    private fun updateUserList(users: List<User>) {
        displayedUsers.clear()
        displayedUsers.addAll(users)
        val currentUser = UserManager.getUserById(loggedInUserId)

        val names = users.map { user ->
            if (currentUser != null && currentUser.friends.contains(user.id)) {
                "${user.username} (Friend)"
            } else user.username
        }

        usersAdapter.clear()
        usersAdapter.addAll(names)
        usersAdapter.notifyDataSetChanged()
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
