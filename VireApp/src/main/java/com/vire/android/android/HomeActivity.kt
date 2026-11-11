package com.vire.android.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class HomeActivity : BaseActivity() {

    private lateinit var feedListView: ListView
    private lateinit var feedAdapter: PostAdapter
    private lateinit var createPostPrompt: TextView
    private var allFeedPosts: List<Post> = emptyList()
    private var selectedFeed = "For you"
    private var blockedUserIds: Set<String> = emptySet()

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

        // Load username immediately from SharedPreferences
        loadUsernameFromSharedPrefs()

        // Refresh username from Firestore asynchronously
        refreshUsernameFromFirestore()

        feedListView = findViewById(R.id.feedListView)
        feedAdapter = PostAdapter(this, FeedManager.getGlobalFeed().toMutableList())
        feedListView.adapter = feedAdapter

        findViewById<ImageView>(R.id.userAvatar)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<View>(R.id.btnHomeNotifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        createPostPrompt = findViewById(R.id.createPostPrompt)
        val openComposer = {
            createPostLauncher.launch(Intent(this, NewPostActivity::class.java))
        }
        createPostPrompt.setOnClickListener { openComposer() }
        findViewById<View>(R.id.btnHomeCreatePost)?.setOnClickListener { openComposer() }

        findViewById<TextView>(R.id.tabHomeForYou).setOnClickListener { setFeedFilter("For you") }
        findViewById<TextView>(R.id.tabHomeFollowing).setOnClickListener { setFeedFilter("Following") }
        findViewById<TextView>(R.id.tabHomeLocal).setOnClickListener { setFeedFilter("Local") }
        findViewById<View>(R.id.cardHomeGameNight).setOnClickListener {
            startActivity(Intent(this, GameNightListActivity::class.java))
        }

        setupStandardBottomNav(BottomTab.HOME)
        loadFeaturedGameNight()
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
        FeedManager.fetchGlobalFeed { list ->
            allFeedPosts = list
            TrustSafetyManager.getBlockedUserIds { blocked ->
                blockedUserIds = blocked
                applySelectedFeed()
            }
        }
    }

    private fun setFeedFilter(filter: String) {
        selectedFeed = filter
        listOf(
            R.id.tabHomeForYou to "For you",
            R.id.tabHomeFollowing to "Following",
            R.id.tabHomeLocal to "Local"
        ).forEach { (id, label) ->
            val tab = findViewById<TextView>(id)
            val selected = selectedFeed == label
            tab.setBackgroundResource(if (selected) R.drawable.pill_tab_active else R.drawable.pill_tab_inactive)
            tab.setTextColor(getColor(if (selected) R.color.circuit_lime else R.color.circuit_text_secondary))
        }
        applySelectedFeed()
    }

    private fun applySelectedFeed() {
        val visiblePosts = allFeedPosts.filter { it.authorUid !in blockedUserIds }
        when (selectedFeed) {
            "Following" -> FriendManager.getFriends { friendIds ->
                showFeedPosts(visiblePosts.filter { it.authorUid in friendIds })
            }
            "Local" -> showFeedPosts(
                visiblePosts.filter {
                    it.postType.contains("game night", ignoreCase = true) ||
                            it.postType.contains("local", ignoreCase = true) ||
                            it.postType.contains("looking for players", ignoreCase = true)
                }
            )
            else -> showFeedPosts(visiblePosts)
        }
    }

    private fun showFeedPosts(filtered: List<Post>) {
        feedAdapter.posts.clear()
        feedAdapter.posts.addAll(filtered)
        feedAdapter.notifyDataSetChanged()
    }

    private fun loadFeaturedGameNight() {
        GameNightManager.getGameNights { gameNights ->
            val nextGameNight = gameNights.firstOrNull() ?: return@getGameNights
            findViewById<TextView>(R.id.tvHomeGameNightTitle).text =
                "${nextGameNight.gameTitle} · ${nextGameNight.time}"
            findViewById<TextView>(R.id.tvHomeGameNightSubtitle).text =
                "${nextGameNight.location} · ${nextGameNight.participants.size} players"
        }
    }
}
