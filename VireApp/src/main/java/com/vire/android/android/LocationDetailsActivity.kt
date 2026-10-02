package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.vire.android.R

class LocationDetailsActivity : BaseActivity() {

    private lateinit var ivImage: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvTypeBadge: TextView
    private lateinit var tvClaimedBadge: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvPhone: TextView
    private lateinit var btnClaim: Button
    private lateinit var btnWebsite: Button
    private lateinit var tvGames: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnWriteReview: Button
    private lateinit var recyclerReviews: RecyclerView
    private lateinit var reviewAdapter: StoreReviewAdapter

    private var currentLocation: GamingLocation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_details)

        setupHamburgerMenu()

        @Suppress("DEPRECATION")
        currentLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("location", GamingLocation::class.java)
        } else {
            intent.getSerializableExtra("location") as? GamingLocation
        }
        if (currentLocation == null) {
            Toast.makeText(this, "Store details not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ivImage = findViewById(R.id.detailsImage)
        tvName = findViewById(R.id.detailsName)
        tvTypeBadge = findViewById(R.id.detailsTypeBadge)
        tvClaimedBadge = findViewById(R.id.detailsClaimedBadge)
        tvRating = findViewById(R.id.detailsRating)
        tvAddress = findViewById(R.id.detailsAddress)
        tvPhone = findViewById(R.id.detailsPhone)
        btnClaim = findViewById(R.id.btnClaimStore)
        btnWebsite = findViewById(R.id.btnVisitWebsite)
        tvGames = findViewById(R.id.detailsGames)
        tvDescription = findViewById(R.id.detailsDescription)
        btnWriteReview = findViewById(R.id.btnWriteReview)
        recyclerReviews = findViewById(R.id.recyclerStoreReviews)

        recyclerReviews.layoutManager = LinearLayoutManager(this)
        reviewAdapter = StoreReviewAdapter(emptyList())
        recyclerReviews.adapter = reviewAdapter

        btnClaim.setOnClickListener {
            attemptClaimStore()
        }

        btnWebsite.setOnClickListener {
            val url = currentLocation?.websiteUrl ?: ""
            if (url.isNotBlank()) {
                val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else url
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to open website link", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No website link provided for this store", Toast.LENGTH_SHORT).show()
            }
        }

        btnWriteReview.setOnClickListener {
            openAddReviewDialog()
        }

        bindLocationDetails()
        loadReviews()
    }

    private fun bindLocationDetails() {
        val location = currentLocation ?: return

        tvName.text = location.name
        tvTypeBadge.text = location.type

        if (location.isClaimed) {
            tvClaimedBadge.visibility = View.VISIBLE
            btnClaim.text = "✓ Verified Store Owner"
            btnClaim.isEnabled = false
        } else {
            tvClaimedBadge.visibility = View.GONE
            btnClaim.text = "Claim Store Page"
            btnClaim.isEnabled = true
        }

        val avg = location.averageRating()
        tvRating.text = "★ ${"%.1f".format(avg)} (${location.reviewCount} reviews)"

        val fullAddr = listOf(location.address, location.city, location.state)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        tvAddress.text = if (fullAddr.isNotBlank()) "📍 $fullAddr" else "📍 Address not specified"

        tvPhone.text = if (location.phoneNumber.isNotBlank()) "📞 ${location.phoneNumber}" else "📞 Phone not provided"

        if (location.supportedGames.isNotEmpty()) {
            tvGames.text = location.supportedGames.joinToString(", ")
        } else {
            tvGames.text = "All Tabletop & Card Games"
        }

        tvDescription.text = location.description.ifBlank { "No detailed store description provided yet." }

        if (location.imageUrl.isNotBlank()) {
            if (location.imageUrl.startsWith("http://") || location.imageUrl.startsWith("https://")) {
                Picasso.get()
                    .load(location.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(ivImage)
            } else {
                try {
                    ivImage.setImageURI(Uri.parse(location.imageUrl))
                } catch (e: Exception) {
                    ivImage.setImageResource(R.drawable.ic_placeholder)
                }
            }
        } else {
            ivImage.setImageResource(R.drawable.ic_placeholder)
        }
    }

    private fun loadReviews() {
        val locationId = currentLocation?.id ?: return
        LocationManager.getReviewsForLocation(locationId) { reviews ->
            reviewAdapter.submitList(reviews)
        }
    }

    private fun attemptClaimStore() {
        val location = currentLocation ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

        AlertDialog.Builder(this)
            .setTitle("Claim Store Page")
            .setMessage("Are you the owner or manager of ${location.name}? Claiming this page gives you official verified status for hosting events.")
            .setPositiveButton("Claim Page") { _, _ ->
                LocationManager.claimLocation(location.id, currentUid) { success ->
                    if (success) {
                        Toast.makeText(this, "Store page claimed successfully!", Toast.LENGTH_SHORT).show()
                        currentLocation = location.copy(isClaimed = true, claimedByUid = currentUid)
                        bindLocationDetails()
                    } else {
                        Toast.makeText(this, "Failed to claim store page", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAddReviewDialog() {
        val location = currentLocation ?: return
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_store_review, null)

        val ratingBar = view.findViewById<RatingBar>(R.id.dialogRatingBar)
        val etComment = view.findViewById<EditText>(R.id.dialogReviewComment)

        AlertDialog.Builder(this)
            .setTitle("Write Store Review")
            .setView(view)
            .setPositiveButton("Submit") { _, _ ->
                val rating = ratingBar.rating
                val comment = etComment.text.toString().trim()
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"
                val username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    .getString("username", "Gamer") ?: "Gamer"

                val review = StoreReview(
                    locationId = location.id,
                    authorUid = uid,
                    authorUsername = username,
                    rating = rating,
                    comment = comment
                )

                LocationManager.addReview(review) { success ->
                    if (success) {
                        Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show()
                        // Update local stats
                        val newSum = location.ratingSum + rating
                        val newCount = location.reviewCount + 1
                        currentLocation = location.copy(ratingSum = newSum, reviewCount = newCount)
                        bindLocationDetails()
                        loadReviews()
                    } else {
                        Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
