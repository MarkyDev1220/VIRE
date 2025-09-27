package com.example.vire.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeActivity : AppCompatActivity() {

    private lateinit var feedListView: ListView
    private val feedPosts = mutableListOf<String>()
    private lateinit var feedAdapter: ArrayAdapter<String>

    companion object {
        const val REQUEST_CODE_NEW_POST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize feed ListView
        feedListView = findViewById(R.id.feedListView)
        feedAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, feedPosts)
        feedListView.adapter = feedAdapter

        // Floating Add Post Button (+)
        val fabAddPost = findViewById<FloatingActionButton>(R.id.fabAddPost)
        fabAddPost.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_NEW_POST)
        }

        // Hamburger menu button
        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener {
            showHamburgerMenu(it)
        }
    }

    // Receive new post from CreatePostActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_NEW_POST && resultCode == Activity.RESULT_OK) {
            val newPost = data?.getStringExtra("new_post")
            newPost?.let {
                feedPosts.add(0, it) // Add to top of feed
                feedAdapter.notifyDataSetChanged()
            }
        }
    }

    // Show popup hamburger menu
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
        popup.show()
    }
}
