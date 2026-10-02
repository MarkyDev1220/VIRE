package com.vire.android.android

import android.app.AlertDialog
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class CommunityDetailsActivity : BaseActivity() {

    private lateinit var tvName: TextView
    private lateinit var btnJoinLeave: Button
    private lateinit var tvCategory: TextView
    private lateinit var tvMembers: TextView
    private lateinit var tvDesc: TextView
    private lateinit var btnCreatePost: Button
    private lateinit var emptyPostsText: TextView
    private lateinit var recyclerPosts: RecyclerView
    private lateinit var postAdapter: CommunityPostAdapter

    private var currentCommunity: Community? = null

    private val postTypes = listOf("Discussion", "Deck Post", "Collection Showcase", "LFG / Looking for Game")

    private var pendingImageUri: Uri? = null
    private var currentCreateDialog: AlertDialog? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImageUri = it
            val iv = currentCreateDialog?.findViewById<ImageView>(R.id.postImagePreview)
            iv?.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_details)

        setupHamburgerMenu()

        @Suppress("DEPRECATION")
        currentCommunity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("community", Community::class.java)
        } else {
            intent.getSerializableExtra("community") as? Community
        }

        if (currentCommunity == null) {
            Toast.makeText(this, "Community details not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvName = findViewById(R.id.detailsCommunityName)
        btnJoinLeave = findViewById(R.id.btnJoinLeaveCommunity)
        tvCategory = findViewById(R.id.detailsCommunityCategory)
        tvMembers = findViewById(R.id.detailsCommunityMembers)
        tvDesc = findViewById(R.id.detailsCommunityDesc)
        btnCreatePost = findViewById(R.id.btnCreatePost)
        emptyPostsText = findViewById(R.id.emptyCommunityPostsText)
        recyclerPosts = findViewById(R.id.recyclerCommunityPosts)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

        recyclerPosts.layoutManager = LinearLayoutManager(this)
        postAdapter = CommunityPostAdapter(
            initial = emptyList(),
            currentUid = currentUid,
            onLikeClick = { post ->
                val community = currentCommunity ?: return@CommunityPostAdapter
                CommunityManager.toggleLikePost(post.id, community.id, currentUid) {
                    loadPosts()
                }
            }
        )
        recyclerPosts.adapter = postAdapter

        btnJoinLeave.setOnClickListener {
            handleJoinToggle()
        }

        btnCreatePost.setOnClickListener {
            openCreatePostDialog()
        }

        bindCommunityDetails()
        loadPosts()
    }

    private fun bindCommunityDetails() {
        val c = currentCommunity ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

        tvName.text = c.name
        tvCategory.text = c.category
        tvMembers.text = "👥 ${c.members.size} Members"
        tvDesc.text = c.description.ifBlank { "No detailed community description." }

        val isMember = c.members.contains(currentUid)
        if (isMember) {
            btnJoinLeave.text = "Joined ✓"
            btnJoinLeave.setBackgroundColor(Color.parseColor("#455A64"))
        } else {
            btnJoinLeave.text = "Join Community"
            btnJoinLeave.setBackgroundColor(Color.parseColor("#1565C0"))
        }
    }

    private fun loadPosts() {
        val communityId = currentCommunity?.id ?: return
        CommunityManager.getPostsForCommunity(communityId) { posts ->
            postAdapter.submitList(posts)
            emptyPostsText.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun handleJoinToggle() {
        val c = currentCommunity ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
        val isMember = c.members.contains(currentUid)

        if (isMember) {
            CommunityManager.leaveCommunity(c.id, currentUid) { success ->
                if (success) {
                    Toast.makeText(this, "Left community", Toast.LENGTH_SHORT).show()
                    val newMembers = c.members.filter { it != currentUid }
                    currentCommunity = c.copy(members = newMembers, memberCount = newMembers.size)
                    bindCommunityDetails()
                }
            }
        } else {
            CommunityManager.joinCommunity(c.id, currentUid) { success ->
                if (success) {
                    Toast.makeText(this, "Joined community!", Toast.LENGTH_SHORT).show()
                    val newMembers = c.members + currentUid
                    currentCommunity = c.copy(members = newMembers, memberCount = newMembers.size)
                    bindCommunityDetails()
                }
            }
        }
    }

    private fun openCreatePostDialog() {
        val c = currentCommunity ?: return
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_community_post, null)

        val spType = view.findViewById<Spinner>(R.id.spinnerPostType)
        val etContent = view.findViewById<EditText>(R.id.editPostContent)
        val btnPickPhoto = view.findViewById<Button>(R.id.btnPickPostPhoto)

        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, postTypes).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        pendingImageUri = null

        btnPickPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("New Post in ${c.name}")
            .setView(view)
            .setPositiveButton("Post") { _, _ ->
                val content = etContent.text.toString().trim()
                if (content.isEmpty()) {
                    Toast.makeText(this, "Post content cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val type = spType.selectedItem?.toString() ?: "Discussion"
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
                val username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    .getString("username", "Gamer") ?: "Gamer"

                // Fetch user avatar
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val avatarUrl = doc.getString("profileImageUrl") ?: ""
                        val post = CommunityPost(
                            communityId = c.id,
                            authorUid = uid,
                            authorName = username,
                            authorAvatarUrl = avatarUrl,
                            content = content,
                            postType = type,
                            imageUrl = pendingImageUri?.toString() ?: ""
                        )

                        CommunityManager.createPost(post) { success, _ ->
                            if (success) {
                                Toast.makeText(this, "Post published!", Toast.LENGTH_SHORT).show()
                                loadPosts()
                            }
                        }
                    }
                    .addOnFailureListener {
                        val post = CommunityPost(
                            communityId = c.id,
                            authorUid = uid,
                            authorName = username,
                            content = content,
                            postType = type,
                            imageUrl = pendingImageUri?.toString() ?: ""
                        )
                        CommunityManager.createPost(post) { success, _ ->
                            if (success) {
                                Toast.makeText(this, "Post published!", Toast.LENGTH_SHORT).show()
                                loadPosts()
                            }
                        }
                    }
            }
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()
        currentCreateDialog = dialog
        dialog.show()
    }
}
