package com.vire.android.android

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.vire.android.R

class ProfileActivity : BaseActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var coverPhoto: ImageView
    private lateinit var changeProfilePicButton: ImageButton
    private lateinit var changeCoverButton: ImageButton
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var aboutMeText: TextView
    private lateinit var gamesText: TextView
    private lateinit var genderText: TextView

    private var selectedProfileUri: Uri? = null
    private var selectedCoverUri: Uri? = null
    private var changingCoverPhoto = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (changingCoverPhoto) {
                selectedCoverUri = uri
                coverPhoto.setImageURI(uri)
                uploadImageToStorage(uri, "coverImageUrl")
            } else {
                selectedProfileUri = uri
                profileImage.setImageURI(uri)
                uploadImageToStorage(uri, "profileImageUrl")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // ⭐ Initialize Hamburger Menu from BaseActivity
        setupHamburgerMenu()

        profileImage = findViewById(R.id.profileImage)
        coverPhoto = findViewById(R.id.coverPhoto)
        changeProfilePicButton = findViewById(R.id.changeProfilePicButton)
        changeCoverButton = findViewById(R.id.changeCoverButton)
        usernameText = findViewById(R.id.profileUsername)
        emailText = findViewById(R.id.profileEmail)
        aboutMeText = findViewById(R.id.profileAboutMe)
        gamesText = findViewById(R.id.profileGames)
        genderText = findViewById(R.id.profileGender)

        val uid = intent.getStringExtra("uid") ?: FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) loadUserProfile(uid)

        changeProfilePicButton.setOnClickListener {
            changingCoverPhoto = false
            pickImageLauncher.launch("image/*")
        }

        changeCoverButton.setOnClickListener {
            changingCoverPhoto = true
            pickImageLauncher.launch("image/*")
        }

        // Initialize social icons (kept from original for better UX)
        findViewById<ImageButton>(R.id.socialDiscord)?.setOnClickListener { openSocialLink("https://discord.com/users/123456") }
        findViewById<ImageButton>(R.id.socialTiktok)?.setOnClickListener { openSocialLink("https://www.tiktok.com/@example") }
        findViewById<ImageButton>(R.id.socialInstagram)?.setOnClickListener { openSocialLink("https://instagram.com/example") }
    }

    // ⭐ Load user profile from Firestore
    private fun loadUserProfile(uid: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                usernameText.text = "Username: ${doc.getString("username") ?: "Unknown"}"
                emailText.text = "Email: ${doc.getString("email") ?: "Unknown"}"
                genderText.text = "Gender: ${doc.getString("gender") ?: "Not set"}"
                aboutMeText.text = "About Me: ${doc.getString("aboutMe") ?: "No bio yet"}"

                val games = (doc.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                gamesText.text = "Games: ${games.joinToString(", ")}"

                val profileUrl = doc.getString("profileImageUrl")
                val coverUrl = doc.getString("coverImageUrl")

                if (!profileUrl.isNullOrEmpty()) {
                    Picasso.get().load(profileUrl).into(profileImage)
                }

                if (!coverUrl.isNullOrEmpty()) {
                    Picasso.get().load(coverUrl).into(coverPhoto)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    // ⭐ Upload image to Firebase Storage
    private fun uploadImageToStorage(uri: Uri, fieldName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference
            .child("user_images/$uid/$fieldName.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    updateImageUrlInFirestore(uid, fieldName, downloadUrl.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    // ⭐ Save image URL to Firestore
    private fun updateImageUrlInFirestore(uid: String, fieldName: String, url: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid)
            .update(fieldName, url)
            .addOnSuccessListener {
                Toast.makeText(this, "Image updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update image URL", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openSocialLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }
}
