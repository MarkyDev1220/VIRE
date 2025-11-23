package com.example.vire.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.activity.result.contract.ActivityResultContracts

class HomeActivity : BaseActivity() {

    private lateinit var feedListView: ListView
    private lateinit var feedAdapter: PostAdapter

    private val createPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshFeed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        feedListView = findViewById(R.id.feedListView)

        // Feed now uses custom adapter to support like + comment
        feedAdapter = PostAdapter(this, FeedManager.getGlobalFeed().toMutableList())

        feedListView.adapter = feedAdapter

        findViewById<FloatingActionButton>(R.id.fabAddPost).setOnClickListener {
            startActivity(Intent(this, NewPostActivity::class.java))
        }

        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener {
            showHamburgerMenu(it)
        }

        refreshFeed()
    }

    override fun onResume() {
        super.onResume()
        refreshFeed()
    }

    private fun refreshFeed() {
        // Since the list is shared in memory, the adapter just needs to refresh
        feedAdapter.notifyDataSetChanged()
    }

    private fun showHamburgerMenu(anchor: android.view.View) {
        val popup = android.widget.PopupMenu(this, anchor)
        popup.menu.apply {
            popup.menu.add("Home")
            popup.menu.add("Profile")
            popup.menu.add("Messages")
            popup.menu.add("Buy/Sell")
            popup.menu.add("Challenges")
            popup.menu.add("Quest")
            popup.menu.add("Settings")
            popup.menu.add("Tournaments")
            popup.menu.add("Rankings")
            popup.menu.add("Friends")
            popup.menu.add("Search")
        }

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

