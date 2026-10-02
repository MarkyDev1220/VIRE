package com.vire.android.android

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso.Picasso
import com.vire.android.R

class CollectionActivity : BaseActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: CollectionAdapter
    private lateinit var searchView: SearchView
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerStatus: Spinner
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyText: TextView
    private lateinit var countText: TextView

    private val typeFilters = listOf("All", "Game", "Deck", "Expansion")
    private val statusFilters = listOf("All", "OWNED", "WANT", "TRADING", "SELLING")

    private val categories = listOf("Board Game", "TCG", "Card Game", "RPG", "Miniatures", "Other")
    private val itemTypes = listOf("Game", "Deck", "Expansion")
    private val statusTags = listOf("OWNED", "WANT", "TRADING", "SELLING")

    private var allCollectionItems = listOf<CollectionItem>()

    private var pendingImageUri: Uri? = null
    private var currentDialog: AlertDialog? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImageUri = it
            val iv = currentDialog?.findViewById<ImageView>(R.id.itemImagePreview)
            iv?.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection)

        setupHamburgerMenu()

        recycler = findViewById(R.id.recyclerCollection)
        searchView = findViewById(R.id.searchViewCollection)
        spinnerType = findViewById(R.id.spinnerFilterType)
        spinnerStatus = findViewById(R.id.spinnerFilterStatus)
        fabAdd = findViewById(R.id.fabAddCollectionItem)
        emptyText = findViewById(R.id.emptyCollectionText)
        countText = findViewById(R.id.collectionCountText)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = CollectionAdapter(emptyList()) { item, action ->
            when (action) {
                CollectionAdapter.Action.VIEW -> showItemDetailsDialog(item)
                CollectionAdapter.Action.EDIT -> openAddEditDialog(item)
                CollectionAdapter.Action.DELETE -> confirmDeleteItem(item)
                CollectionAdapter.Action.CHANGE_STATUS -> openAddEditDialog(item)
            }
        }
        recycler.adapter = adapter

        // Setup Spinners
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeFilters).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusFilters).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerType.onItemSelectedListener = filterListener
        spinnerStatus.onItemSelectedListener = filterListener

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilters()
                return true
            }
        })

        fabAdd.setOnClickListener {
            openAddEditDialog(null)
        }

        loadCollection()
    }

    private fun loadCollection() {
        CollectionManager.getUserCollection { items ->
            allCollectionItems = items
            applyFilters()
        }
    }

    private fun applyFilters() {
        val query = searchView.query?.toString() ?: ""
        val type = spinnerType.selectedItem?.toString() ?: "All"
        val status = spinnerStatus.selectedItem?.toString() ?: "All"

        val filtered = CollectionManager.filterCollection(allCollectionItems, query, type, status)
        adapter.submitList(filtered)

        countText.text = "${filtered.size} items"
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openAddEditDialog(existingItem: CollectionItem?) {
        val isEdit = existingItem != null
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_collection_item, null)

        val etTitle = view.findViewById<EditText>(R.id.editItemTitle)
        val spType = view.findViewById<Spinner>(R.id.spinnerItemType)
        val spStatus = view.findViewById<Spinner>(R.id.spinnerStatusTag)
        val spCategory = view.findViewById<Spinner>(R.id.spinnerCategory)
        val etPrice = view.findViewById<EditText>(R.id.editItemPrice)
        val etNotes = view.findViewById<EditText>(R.id.editItemNotes)
        val ivPreview = view.findViewById<ImageView>(R.id.itemImagePreview)
        val btnChoosePhoto = view.findViewById<Button>(R.id.btnPickImage)

        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, itemTypes).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusTags).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        pendingImageUri = null

        if (existingItem != null) {
            etTitle.setText(existingItem.title)
            etNotes.setText(existingItem.notes)
            if (existingItem.price > 0.0) etPrice.setText(existingItem.price.toString())

            val typeIndex = itemTypes.indexOf(existingItem.itemType)
            if (typeIndex >= 0) spType.setSelection(typeIndex)

            val statusIndex = statusTags.indexOf(existingItem.statusTag)
            if (statusIndex >= 0) spStatus.setSelection(statusIndex)

            val catIndex = categories.indexOf(existingItem.category)
            if (catIndex >= 0) spCategory.setSelection(catIndex)

            if (existingItem.imageUrl.isNotBlank()) {
                if (existingItem.imageUrl.startsWith("http://") || existingItem.imageUrl.startsWith("https://")) {
                    Picasso.get().load(existingItem.imageUrl).into(ivPreview)
                } else {
                    try {
                        ivPreview.setImageURI(Uri.parse(existingItem.imageUrl))
                    } catch (e: Exception) {
                        ivPreview.setImageResource(R.drawable.ic_placeholder)
                    }
                }
            }
        }

        btnChoosePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Collection Item" else "Add to Collection")
            .setView(view)
            .setPositiveButton(if (isEdit) "Save" else "Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val itemType = spType.selectedItem?.toString() ?: "Game"
                val statusTag = spStatus.selectedItem?.toString() ?: "OWNED"
                val category = spCategory.selectedItem?.toString() ?: "Board Game"
                val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                val notes = etNotes.text.toString().trim()
                val imageUrl = pendingImageUri?.toString() ?: existingItem?.imageUrl ?: ""

                val newItem = CollectionItem(
                    id = existingItem?.id ?: "",
                    userId = existingItem?.userId ?: "",
                    title = title,
                    itemType = itemType,
                    statusTag = statusTag,
                    category = category,
                    price = price,
                    notes = notes,
                    imageUrl = imageUrl,
                    createdAt = existingItem?.createdAt ?: System.currentTimeMillis()
                )

                if (isEdit) {
                    CollectionManager.updateCollectionItem(newItem) { success ->
                        if (success) {
                            Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
                            loadCollection()
                        } else {
                            Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    CollectionManager.addCollectionItem(newItem) { success, _ ->
                        if (success) {
                            Toast.makeText(this, "Added to collection", Toast.LENGTH_SHORT).show()
                            loadCollection()
                        } else {
                            Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()
        currentDialog = dialog
        dialog.show()
    }

    private fun showItemDetailsDialog(item: CollectionItem) {
        val builder = AlertDialog.Builder(this)
            .setTitle(item.title)
            .setMessage(
                "Type: ${item.itemType}\n" +
                        "Status: ${item.statusTag}\n" +
                        "Category: ${item.category}\n" +
                        (if (item.price > 0.0) "Est. Value: $${"%.2f".format(item.price)}\n" else "") +
                        (if (item.notes.isNotBlank()) "\nNotes:\n${item.notes}" else "")
            )
            .setPositiveButton("Close", null)
            .setNeutralButton("Edit") { _, _ ->
                openAddEditDialog(item)
            }
        builder.show()
    }

    private fun confirmDeleteItem(item: CollectionItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${item.title}?")
            .setMessage("Are you sure you want to remove this item from your collection?")
            .setPositiveButton("Delete") { _, _ ->
                CollectionManager.deleteCollectionItem(item.id) { success ->
                    if (success) {
                        Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
                        loadCollection()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
