package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vire.android.R

class BuySellActivity : BaseActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: MarketplaceAdapter
    private lateinit var spinnerCategory: Spinner
    private lateinit var searchView: SearchView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyText: TextView

    // Category options
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

    private var pendingImageUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImageUri = it
            val iv = currentCreateDialog?.findViewById<ImageView>(R.id.createItemImage)
            iv?.setImageURI(it)
        }
    }
    private var currentCreateDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_sell)

        setupHamburgerMenu()
        findViewById<TextView>(R.id.buySellText).text = "Buy/Sell Page"

        recycler = findViewById(R.id.recyclerMarketplace)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        searchView = findViewById(R.id.searchViewMarketplace)
        fabAdd = findViewById(R.id.fabAddListing)
        emptyText = findViewById(R.id.emptyText)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = MarketplaceAdapter(MarketplaceManager.getListings()) { item, action ->
            when (action) {
                MarketplaceAdapter.Action.VIEW -> showItemDetails(item)
                MarketplaceAdapter.Action.BUY -> attemptBuy(item)
                MarketplaceAdapter.Action.DELETE -> {
                    MarketplaceManager.deleteListing(item.id)
                    refreshListings()
                }
            }
        }
        recycler.adapter = adapter

        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterAndSearch()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { filterAndSearch(); return true }
            override fun onQueryTextChange(newText: String?): Boolean { filterAndSearch(); return true }
        })

        fabAdd.setOnClickListener { openCreateListingDialog() }

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
        tvPrice.text = String.format("$%.2f", item.price)
        if (item.imageUri != null) iv.setImageURI(Uri.parse(item.imageUri)) else iv.setImageResource(R.drawable.ic_placeholder)
        builder.setView(view)
        builder.setPositiveButton("Buy") { _, _ -> attemptBuy(item) }
        builder.setNegativeButton("Close", null)
        builder.show()
    }

    private fun attemptBuy(item: MarketplaceItem) {
        AlertDialog.Builder(this)
            .setTitle("Buy ${item.title}?")
            .setMessage(String.format("Price: $%.2f\nThis demo will mark the listing as sold.", item.price))
            .setPositiveButton("Confirm") { _, _ ->
                MarketplaceManager.markSold(item.id)
                refreshListings()
                Toast.makeText(this, "Marked as sold", Toast.LENGTH_SHORT).show()
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
