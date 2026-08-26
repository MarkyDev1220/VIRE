package com.example.vire.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts

class HomeActivity : BaseActivity() {

    private lateinit var feedListView: ListView
    private lateinit var feedAdapter: PostAdapter

    private lateinit var createPostPrompt: TextView
    private lateinit var navHome: ImageButton
    private lateinit var navProfile: ImageButton
    private lateinit var navCreatePost: ImageButton
    private lateinit var navFriends: ImageButton
    private lateinit var navMenu: ImageButton

    private val createPostLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refreshFeed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // FEED LIST
        feedListView = findViewById(R.id.feedListView)

        feedAdapter = PostAdapter(
            this,
            FeedManager.getGlobalFeed().toMutableList()
        )
        feedListView.adapter = feedAdapter

        // POST COMPOSER ("What's on your mind?")
        createPostPrompt = findViewById(R.id.createPostPrompt)
        createPostPrompt.setOnClickListener {
            val intent = Intent(this, NewPostActivity::class.java)
            intent.putExtra("username", "User")
            createPostLauncher.launch(intent)
        }

        // BOTTOM NAV BUTTONS
        navHome = findViewById(R.id.navHome)
        navProfile = findViewById(R.id.navProfile)
        navCreatePost = findViewById(R.id.navCreatePost)
        navFriends = findViewById(R.id.navFriends)
        navMenu = findViewById(R.id.navMenu)

        setupBottomNav()

        refreshFeed()
    }

    override fun onResume() {
        super.onResume()
        refreshFeed()
    }

    private fun refreshFeed() {
        feedAdapter.apply {
            posts.clear()
            posts.addAll(FeedManager.getGlobalFeed())
            notifyDataSetChanged()
        }
    }

    private fun setupBottomNav() {

        navHome.setOnClickListener {
            // Already on Home — no navigation needed
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        navCreatePost.setOnClickListener {
            val intent = Intent(this, NewPostActivity::class.java)
            intent.putExtra("username", "User")
            createPostLauncher.launch(intent)
        }

        navFriends.setOnClickListener {
            startActivity(Intent(this, FriendsActivity::class.java))
        }

        navMenu.setOnClickListener { anchor ->
            val popup = android.widget.PopupMenu(this, anchor)
            popup.menu.apply {

                add("Messages")
                add("Buy/Sell")
                add("Challenges")
                add("Quest")
                add("Settings")
                add("Tournaments")
                add("Rankings")

                add("Search")
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {


                    "Messages" -> startActivity(Intent(this, MessagesActivity::class.java))
                    "Buy/Sell" -> startActivity(Intent(this, BuySellActivity::class.java))
                    "Challenges" -> startActivity(Intent(this, ChallengesActivity::class.java))
                    "Quest" -> startActivity(Intent(this, QuestActivity::class.java))
                    "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                    "Tournaments" -> startActivity(Intent(this, TournamentsActivity::class.java))
                    "Rankings" -> startActivity(Intent(this, RankingsActivity::class.java))

                    "Search" -> startActivity(Intent(this, SearchActivity::class.java))
                }
                true
            }

            popup.show()
        }
    }
}

