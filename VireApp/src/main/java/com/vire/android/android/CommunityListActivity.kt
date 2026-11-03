package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.vire.android.R

class CommunityListActivity : BaseActivity() {

    private lateinit var spinnerCategory: Spinner
    private lateinit var recycler: RecyclerView
    private lateinit var fabCreate: FloatingActionButton
    private lateinit var adapter: CommunityAdapter
    private lateinit var searchInput: EditText

    private val categories = listOf("All", "TCG", "RPG", "Miniature Wargame", "Board Game", "Other")
    private val communityCategories = listOf("TCG", "RPG", "Miniature Wargame", "Board Game", "Other")

    private var allCommunities = listOf<Community>()

    private var pendingImageUri: Uri? = null
    private var currentCreateDialog: AlertDialog? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImageUri = it
            val iv = currentCreateDialog?.findViewById<ImageView>(R.id.communityImagePreview)
            iv?.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_list)

        setupStandardBottomNav(BottomTab.COMMUNITIES)

        spinnerCategory = findViewById(R.id.spinnerCommunityCategory)
        recycler = findViewById(R.id.recyclerCommunities)
        fabCreate = findViewById(R.id.fabCreateCommunity)
        searchInput = findViewById(R.id.searchCommunities)

        findViewById<ImageButton>(R.id.btnCommunityNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnCommunityProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filterCommunities()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = CommunityAdapter(
            initial = emptyList(),
            currentUid = currentUid,
            onCommunityClick = { community ->
                val intent = Intent(this, CommunityDetailsActivity::class.java)
                intent.putExtra("community", community)
                startActivity(intent)
            },
            onJoinToggleClick = { community ->
                handleJoinToggle(community)
            }
        )
        recycler.adapter = adapter

        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterCommunities()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        fabCreate.setOnClickListener {
            openCreateCommunityDialog()
        }

        loadCommunities()
    }

    override fun onResume() {
        super.onResume()
        loadCommunities()
    }

    private fun loadCommunities() {
        CommunityManager.getCommunities { list ->
            allCommunities = list
            filterCommunities()
        }
    }

    private fun filterCommunities() {
        val selectedCategory = spinnerCategory.selectedItem?.toString() ?: "All"
        val query = searchInput.text.toString().trim()
        val filtered = allCommunities.filter { community ->
            val matchesCategory = selectedCategory.equals("All", ignoreCase = true) ||
                    community.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    community.name.contains(query, ignoreCase = true) ||
                    community.category.contains(query, ignoreCase = true) ||
                    community.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
        adapter.submitList(filtered)
    }

    private fun handleJoinToggle(community: Community) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
        val isMember = community.members.contains(currentUid)

        if (isMember) {
            CommunityManager.leaveCommunity(community.id, currentUid) { success ->
                if (success) {
                    Toast.makeText(this, "Left ${community.name}", Toast.LENGTH_SHORT).show()
                    loadCommunities()
                }
            }
        } else {
            CommunityManager.joinCommunity(community.id, currentUid) { success ->
                if (success) {
                    Toast.makeText(this, "Joined ${community.name}!", Toast.LENGTH_SHORT).show()
                    loadCommunities()
                }
            }
        }
    }

    private fun openCreateCommunityDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_community, null)

        val etName = view.findViewById<EditText>(R.id.editCommunityName)
        val spCategory = view.findViewById<Spinner>(R.id.spinnerCommunityCategory)
        val etDesc = view.findViewById<EditText>(R.id.editCommunityDescription)
        val btnPickPhoto = view.findViewById<Button>(R.id.btnPickCommunityPhoto)

        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, communityCategories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        pendingImageUri = null

        btnPickPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

        val builder = AlertDialog.Builder(this)
            .setTitle("Create Gaming Community")
            .setView(view)
            .setPositiveButton("Create Community") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Community name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val category = spCategory.selectedItem?.toString() ?: "TCG"
                val description = etDesc.text.toString().trim()

                val newCommunity = Community(
                    name = name,
                    category = category,
                    description = description,
                    bannerUrl = pendingImageUri?.toString() ?: "",
                    members = listOf(currentUid),
                    memberCount = 1
                )

                CommunityManager.createCommunity(newCommunity) { success, _ ->
                    if (success) {
                        Toast.makeText(this, "Community created!", Toast.LENGTH_SHORT).show()
                        loadCommunities()
                    } else {
                        Toast.makeText(this, "Failed to create community", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()
        currentCreateDialog = dialog
        dialog.show()
    }
}
