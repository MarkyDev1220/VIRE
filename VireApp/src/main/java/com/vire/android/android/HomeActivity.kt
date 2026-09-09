package com.vire.android.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class HomeActivity : BaseActivity() {

    private lateinit var feedListView: ListView
    private lateinit var feedAdapter: PostAdapter

    private lateinit var createPostPrompt: TextView
    private lateinit var navHome: ImageButton
    private lateinit var navProfile: ImageButton
    private lateinit var navCreatePost: ImageButton
    private lateinit var navFriends: ImageButton
    private lateinit var navMenu: ImageButton

    private var cachedUsername: String = "User"

    private val createPostLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refreshFeed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        FirebaseApp.initializeApp(this)

        // Load username immediately from SharedPreferences
        loadUsernameFromSharedPrefs()

        // Refresh username from Firestore asynchronously
        refreshUsernameFromFirestore()

        feedListView = findViewById(R.id.feedListView)
        feedAdapter = PostAdapter(this, FeedManager.getGlobalFeed().toMutableList())
        feedListView.adapter = feedAdapter

        createPostPrompt = findViewById(R.id.createPostPrompt)

        // ✅ FIXED: No username passed through Intent
        createPostPrompt.setOnClickListener {
            createPostLauncher.launch(Intent(this, NewPostActivity::class.java))
        }

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
        loadUsernameFromSharedPrefs()
        refreshFeed()
    }

    private fun loadUsernameFromSharedPrefs() {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        cachedUsername = prefs.getString("username", "User") ?: "User"
    }

    private fun refreshUsernameFromFirestore() {
        val auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            return
        }

        val uid = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username")
                if (!username.isNullOrEmpty()) {
                    cachedUsername = username

                    val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("username", username).apply()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not refresh username", Toast.LENGTH_SHORT).show()
            }
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
            // Already on Home
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // ✅ FIXED: No username passed through Intent
        navCreatePost.setOnClickListener {
            createPostLauncher.launch(Intent(this, NewPostActivity::class.java))
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


