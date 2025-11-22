package com.example.vire.android

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton


class BuySellActivity : BaseActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: MarketplaceAdapter
    private lateinit var spinnerCategory: Spinner
    private lateinit var searchView: SearchView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyText: TextView

    // Category options - includes Decks/Cards, Accessories, Items and extras
    private val categories = listOf(
        "All",
        "Decks/Cards",
        "Accessories",
        "Items",
        "Sealed Products",
        "Playmats",
        "Collections",
        "Other"
    )

    // --- Create listing dialog state ---
    private var pendingImageUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImageUri = it
            // show in dialog if currently open (we'll handle attaching view via currentCreateDialog)
            val iv = currentCreateDialog?.findViewById<ImageView>(R.id.createItemImage)
            iv?.setImageURI(it)
        }
    }
    private var currentCreateDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_sell)

        // Hamburger/menu + header text from original activity
        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener {
            val popup = android.widget.PopupMenu(this, it)
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
                when(item.title.toString()) {
                    "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                    "Profile" -> startActivity(Intent(this, ProfileActivity::class.java))
                    "Messages" -> startActivity(Intent(this, MessagesActivity::class.java))
                    "Buy/Sell" -> startActivity(Intent(this, BuySellActivity::class.java))
                    "Challenges" -> startActivity(Intent(this, ChallengesActivity::class.java))
                    "Quest" -> startActivity(Intent(this, QuestActivity::class.java))
                    "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                    "Tournaments" -> startActivity(Intent(this, TournamentsActivity::class.java))
                    "Rankings" -> startActivity(Intent(this, RankingsActivity::class.java))
                }
                true
            }
            popup.show()
        }
        findViewById<TextView>(R.id.buySellText).text = "Buy/Sell Page"

        // Marketplace views
        recycler = findViewById(R.id.recyclerMarketplace)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        searchView = findViewById(R.id.searchViewMarketplace)
        fabAdd = findViewById(R.id.fabAddListing)
        emptyText = findViewById(R.id.emptyText)

        // Recycler setup
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = MarketplaceAdapter(MarketplaceManager.getListings()) { item, action ->
            when (action) {
                MarketplaceAdapter.Action.VIEW -> showItemDetails(item)
                MarketplaceAdapter.Action.BUY -> attemptBuy(item)
                MarketplaceAdapter.Action.DELETE -> {
                    // In a real app check ownership before delete
                    MarketplaceManager.deleteListing(item.id)
                    refreshListings()
                }
            }
        }
        recycler.adapter = adapter

        // Category spinner
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterAndSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterAndSearch()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterAndSearch()
                return true
            }
        })

        // FAB -> create selling listing
        fabAdd.setOnClickListener { openCreateListingDialog() }

        // Initial refresh
        refreshListings()
    }

    private fun refreshListings() {
        val list = MarketplaceManager.getListings()
        adapter.submitList(list)
        emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterAndSearch() {
        val query = searchView.query?.toString()?.trim() ?: ""
        val category = spinnerCategory.selectedItem?.toString() ?: "All"
        val filtered = MarketplaceManager.searchAndFilter(query, category)
        adapter.submitList(filtered)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showItemDetails(item: MarketplaceItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(item.title)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_marketplace_item, null)
        val iv = view.findViewById<ImageView>(R.id.dialogItemImage)
        val tvDesc = view.findViewById<TextView>(R.id.dialogItemDesc)
        val tvPrice = view.findViewById<TextView>(R.id.dialogItemPrice)
        tvDesc.text = item.description
        tvPrice.text = "$${"%.2f".format(item.price)}"
        if (item.imageUri != null) iv.setImageURI(Uri.parse(item.imageUri)) else iv.setImageResource(R.drawable.ic_placeholder)
        builder.setView(view)
        builder.setPositiveButton("Buy") { _, _ -> attemptBuy(item) }
        builder.setNegativeButton("Close", null)
        builder.show()
    }

    private fun attemptBuy(item: MarketplaceItem) {
        // Simple demo buy flow -> mark sold and remove listing
        AlertDialog.Builder(this)
            .setTitle("Buy ${item.title}?")
            .setMessage("Price: $${"%.2f".format(item.price)}\nThis demo will mark the listing as sold.")
            .setPositiveButton("Confirm") { _, _ ->
                MarketplaceManager.markSold(item.id)
                refreshListings()
                Toast.makeText(this, "Marked as sold (demo). Integrate payments/backend next.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openCreateListingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_listing, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.createTitle)
        val etDesc = dialogView.findViewById<EditText>(R.id.createDesc)
        val etPrice = dialogView.findViewById<EditText>(R.id.createPrice)
        val spCategory = dialogView.findViewById<Spinner>(R.id.createCategory)
        val btnPickImage = dialogView.findViewById<ImageButton>(R.id.pickImageButton)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.createItemImage)

        // For create category spinner show meaningful selling categories (exclude "All")
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories.drop(1)).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        pendingImageUri = null
        ivPreview.setImageResource(R.drawable.ic_placeholder)

        btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Create Selling Listing")
            .setView(dialogView)
            .setPositiveButton("Post") { _, _ ->
                val title = etTitle.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                val category = spCategory.selectedItem?.toString() ?: "Other"

                if (title.isEmpty()) {
                    Toast.makeText(this, "Please add a title", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // Create item and add
                val item = MarketplaceItem(
                    id = MarketplaceManager.nextId(),
                    title = title,
                    description = desc,
                    price = price,
                    category = category,
                    imageUri = pendingImageUri?.toString(),
                    owner = "demoUser"
                )
                MarketplaceManager.addListing(item)
                pendingImageUri = null
                refreshListings()
            }
            .setNegativeButton("Cancel") { _, _ -> pendingImageUri = null }

        val dialog = builder.create()
        currentCreateDialog = dialog
        dialog.show()
    }
}
