package com.example.vire.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.activity.result.contract.ActivityResultContracts

class HomeActivity : BaseActivity() {

    private lateinit var feedListView: ListView
    private val feedPosts = mutableListOf<String>()
    private lateinit var feedAdapter: ArrayAdapter<String>

    // Launcher for CreatePostActivity
    private val createPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newPost = result.data?.getStringExtra("new_post")
            newPost?.let {
                feedPosts.add(0, it)
                feedAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        feedListView = findViewById(R.id.feedListView)
        feedAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, feedPosts)
        feedListView.adapter = feedAdapter

        val fabAddPost = findViewById<FloatingActionButton>(R.id.fabAddPost)
        fabAddPost.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            createPostLauncher.launch(intent)
        }

        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener {
            showHamburgerMenu(it)
        }
    }

    private fun showHamburgerMenu(anchor: android.view.View) {
        val popup = android.widget.PopupMenu(this, anchor)
        popup.menu.add("Home")
        popup.menu.add("Profile")
        popup.menu.add("Messages")
        popup.menu.add("Buy/Sell")
        popup.menu.add("Challenges")
        popup.menu.add("Quest")
        popup.menu.add("Settings")
        popup.menu.add("Tournaments")
        popup.menu.add("Rankings")

        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Home" -> true
                "Profile" -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                "Messages" -> { startActivity(Intent(this, MessagesActivity::class.java)); true }
                "Buy/Sell" -> { startActivity(Intent(this, BuySellActivity::class.java)); true }
                "Challenges" -> { startActivity(Intent(this, ChallengesActivity::class.java)); true }
                "Quest" -> { startActivity(Intent(this, QuestActivity::class.java)); true }
                "Settings" -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                "Tournaments" -> { startActivity(Intent(this, TournamentsActivity::class.java)); true }
                "Rankings" -> { startActivity(Intent(this, RankingsActivity::class.java)); true}
                else -> false
            }
        }
        popup.show()
    }
}
