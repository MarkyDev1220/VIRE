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

class LocationListActivity : BaseActivity() {

    private lateinit var searchView: SearchView
    private lateinit var spinnerType: Spinner
    private lateinit var recycler: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var emptyText: TextView
    private lateinit var countText: TextView
    private lateinit var adapter: LocationAdapter

    private val typeFilters = listOf(
        "All",
        "Local Game Store",
        "Card Shop",
        "Board Game Café",
        "Gaming Group",
        "Convention"
    )

    private val locationTypes = listOf(
        "Local Game Store",
        "Card Shop",
        "Board Game Café",
        "Gaming Group",
        "Convention"
    )

    private var allLocations = listOf<GamingLocation>()

    private var pendingImageUri: Uri? = null
    private var currentCreateDialog: AlertDialog? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImageUri = it
            val iv = currentCreateDialog?.findViewById<ImageView>(R.id.locationImagePreview)
            iv?.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_list)

        setupHamburgerMenu()

        searchView = findViewById(R.id.searchViewLocations)
        spinnerType = findViewById(R.id.spinnerLocationTypeFilter)
        recycler = findViewById(R.id.recyclerLocations)
        fabAdd = findViewById(R.id.fabAddLocation)
        emptyText = findViewById(R.id.emptyLocationsText)
        countText = findViewById(R.id.locationCountText)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = LocationAdapter(emptyList()) { location ->
            val intent = Intent(this, LocationDetailsActivity::class.java)
            intent.putExtra("location", location)
            startActivity(intent)
        }
        recycler.adapter = adapter

        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeFilters).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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
            openAddLocationDialog()
        }

        loadLocations()
    }

    override fun onResume() {
        super.onResume()
        loadLocations()
    }

    private fun loadLocations() {
        LocationManager.getLocations { locations ->
            allLocations = locations
            applyFilters()
        }
    }

    private fun applyFilters() {
        val query = searchView.query?.toString() ?: ""
        val typeFilter = spinnerType.selectedItem?.toString() ?: "All"

        val filtered = LocationManager.filterLocations(allLocations, query, typeFilter)
        adapter.submitList(filtered)

        countText.text = "${filtered.size} stores"
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openAddLocationDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_location, null)

        val etName = view.findViewById<EditText>(R.id.editLocationName)
        val spType = view.findViewById<Spinner>(R.id.spinnerLocationType)
        val etAddress = view.findViewById<EditText>(R.id.editLocationAddress)
        val etCity = view.findViewById<EditText>(R.id.editLocationCity)
        val etState = view.findViewById<EditText>(R.id.editLocationState)
        val etPhone = view.findViewById<EditText>(R.id.editLocationPhone)
        val etWebsite = view.findViewById<EditText>(R.id.editLocationWebsite)
        val etGames = view.findViewById<EditText>(R.id.editLocationSupportedGames)
        val etDesc = view.findViewById<EditText>(R.id.editLocationDescription)
        val btnPickPhoto = view.findViewById<Button>(R.id.btnPickLocationPhoto)

        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationTypes).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        pendingImageUri = null

        btnPickPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Add Gaming Store / Location")
            .setView(view)
            .setPositiveButton("Add Location") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Store name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val type = spType.selectedItem?.toString() ?: "Local Game Store"
                val address = etAddress.text.toString().trim()
                val city = etCity.text.toString().trim()
                val state = etState.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val website = etWebsite.text.toString().trim()
                val description = etDesc.text.toString().trim()

                val gamesList = etGames.text.toString()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val newLocation = GamingLocation(
                    name = name,
                    type = type,
                    address = address,
                    city = city,
                    state = state,
                    phoneNumber = phone,
                    websiteUrl = website,
                    description = description,
                    supportedGames = gamesList,
                    imageUrl = pendingImageUri?.toString() ?: ""
                )

                LocationManager.createLocation(newLocation) { success, _ ->
                    if (success) {
                        Toast.makeText(this, "Gaming location added!", Toast.LENGTH_SHORT).show()
                        loadLocations()
                    } else {
                        Toast.makeText(this, "Failed to add location", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()
        currentCreateDialog = dialog
        dialog.show()
    }
}
