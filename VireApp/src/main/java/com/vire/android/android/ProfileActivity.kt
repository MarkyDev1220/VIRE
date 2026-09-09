package com.vire.android.android

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.vire.android.R
import java.util.*

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

    private lateinit var socialDiscord: ImageButton
    private lateinit var socialTiktok: ImageButton
    private lateinit var socialInstagram: ImageButton
    private lateinit var addFriendButton: Button
    private lateinit var profileFeedListView: ListView
    private lateinit var profileFeedAdapter: ArrayAdapter<String>
    private val profileFeedPosts = mutableListOf<String>()

    private lateinit var editPenButton: ImageButton
    private lateinit var hamburgerButton: ImageButton

    private var selectedProfileUri: Uri? = null
    private var selectedCoverUri: Uri? = null
    private var changingCoverPhoto = false

    private var currentUid: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val uid = currentUid ?: return@registerForActivityResult
        uri?.let {
            if (changingCoverPhoto) {
                selectedCoverUri = uri
                profileSafeLoad(uri, coverPhoto)
                uploadImageToStorage(uid, uri, "coverImageUrl")
            } else {
                selectedProfileUri = uri
                profileSafeLoad(uri, profileImage)
                uploadImageToStorage(uid, uri, "profileImageUrl")
            }
        }
    }

    private val pickCustomBackgroundLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val root = findViewById<ScrollView>(R.id.scrollViewRoot)
                root.background = null
                val drawable: Drawable? =
                    Drawable.createFromStream(contentResolver.openInputStream(uri), uri.toString())
                root.background = drawable
                Toast.makeText(this, "Custom theme applied!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load background", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileImage = findViewById(R.id.profileImage)
        coverPhoto = findViewById(R.id.coverPhoto)
        changeProfilePicButton = findViewById(R.id.changeProfilePicButton)
        changeCoverButton = findViewById(R.id.changeCoverButton)
        usernameText = findViewById(R.id.profileUsername)
        emailText = findViewById(R.id.profileEmail)
        aboutMeText = findViewById(R.id.profileAboutMe)
        gamesText = findViewById(R.id.profileGames)
        genderText = findViewById(R.id.profileGender)

        socialDiscord = findViewById(R.id.socialDiscord)
        socialTiktok = findViewById(R.id.socialTiktok)
        socialInstagram = findViewById(R.id.socialInstagram)
        addFriendButton = findViewById(R.id.addFriendButton)
        editPenButton = findViewById(R.id.editPenButton)

        profileFeedListView = findViewById(R.id.profileFeedListView)
        profileFeedAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, profileFeedPosts)
        profileFeedListView.adapter = profileFeedAdapter

        hamburgerButton = findViewById(R.id.hamburgerButton)

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
            popup.menu.add("Friends")
            popup.menu.add("Search")

            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                    "Profile" -> startActivity(Intent(this, ProfileActivity::class.java))
                    "Messages" -> startActivity(Intent(this, MessagesActivity::class.java))
                    "Buy/Sell" -> startActivity(Intent(this, BuySellActivity::class.java))
                    "Challenges" -> startActivity(Intent(this, ChallengesActivity::class.java))
                    "Quest" -> startActivity(Intent(this, QuestActivity::class.java))
                    "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                    "Tournaments" -> startActivity(Intent(this, TournamentsActivity::class.java))
                    "Rankings" -> startActivity(Intent(this, RankingsActivity::class.java))
                    "Friends" -> startActivity(Intent(this, FriendsActivity::class.java))
                    "Search" -> startActivity(Intent(this, SearchActivity::class.java))
                }
                true
            }
            popup.show()
        }

        val explicitUid = intent.getStringExtra("uid")
        val authUser = FirebaseAuth.getInstance().currentUser
        val uid = explicitUid ?: authUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in. Please sign in again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentUid = uid
        loadUserProfile(uid)

        changeProfilePicButton.setOnClickListener {
            changingCoverPhoto = false
            pickImageLauncher.launch("image/*")
        }

        changeCoverButton.setOnClickListener {
            changingCoverPhoto = true
            pickImageLauncher.launch("image/*")
        }

        editPenButton.setOnClickListener { showEditProfileDialog() }

        socialDiscord.setOnClickListener { openSocialLink("https://discord.com/users/123456") }
        socialTiktok.setOnClickListener { openSocialLink("https://www.tiktok.com/@example") }
        socialInstagram.setOnClickListener { openSocialLink("https://instagram.com/example") }
        findViewById<ImageButton>(R.id.socialFacebook)?.setOnClickListener {
            openSocialLink("https://facebook.com/example")
        }
        findViewById<ImageButton>(R.id.socialtwitch)?.setOnClickListener {
            openSocialLink("https://twitch.tv/example")
        }
        findViewById<ImageButton>(R.id.socialkick)?.setOnClickListener {
            openSocialLink("https://kick.com/example")
        }

        addFriendButton.setOnClickListener {
            Toast.makeText(this, "Friend system coming soon.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val loggedInUser = loadUser(this)
            if (loggedInUser != null) {
                profileFeedPosts.clear()
                profileFeedAdapter.notifyDataSetChanged()
            }
        } catch (_: Exception) {}
    }

    private fun loadUserProfile(uid: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                usernameText.text = "Username: ${doc.getString("username") ?: "Unknown"}"
                emailText.text = "Email: ${doc.getString("email") ?: "Unknown"}"
                genderText.text = "Gender: ${doc.getString("gender") ?: "Not set"}"
                aboutMeText.text = "About Me: ${doc.getString("aboutMe") ?: "No bio yet"}"

                val games = (doc.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                gamesText.text = if (games.isNotEmpty()) {
                    "Games: ${games.joinToString(", ")}"
                } else {
                    "Games: None added yet"
                }

                val profileUrl = doc.getString("profileImageUrl")
                val coverUrl = doc.getString("coverImageUrl")

                if (!profileUrl.isNullOrEmpty()) remoteSafeLoad(profileUrl, profileImage)
                if (!coverUrl.isNullOrEmpty()) remoteSafeLoad(coverUrl, coverPhoto)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageToStorage(uid: String, uri: Uri, fieldName: String) {
        try {
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
        } catch (e: Exception) {
            Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

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

    private fun showEditProfileDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
            val usernameInput = dialogView.findViewById<EditText>(R.id.editUsername)
            val emailInput = dialogView.findViewById<EditText>(R.id.editEmail)
            val aboutMeInput = dialogView.findViewById<EditText>(R.id.editAboutMe)
            val gamesInput = dialogView.findViewById<EditText>(R.id.editGames)
            val dobInput = dialogView.findViewById<EditText>(R.id.editDOB)
            val themeBtn = dialogView.findViewById<Button>(R.id.editThemeButtonDialog)

            val user = loadUser(this)
            user?.let {
                usernameInput.setText(it.username)
                emailInput.setText(it.email)
                aboutMeInput.setText(aboutMeText.text.toString().replace("About Me: ", ""))
                gamesInput.setText(it.favoriteGames.joinToString(", "))
                dobInput.setText(it.dateOfBirth)
            }

            dobInput.inputType = InputType.TYPE_NULL
            dobInput.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this,
                    { _, year, month, dayOfMonth -> dobInput.setText("${month + 1}/$dayOfMonth/$year") },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            themeBtn.setOnClickListener { showThemePickerDialog() }

            AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val updatedUser = User(
                        id = 1,
                        username = usernameInput.text.toString(),
                        email = emailInput.text.toString(),
                        gender = user?.gender ?: "",
                        dateOfBirth = dobInput.text.toString(),
                        favoriteGames = gamesInput.text.toString().split(",").map { it.trim() },
                        profileImageUri = user?.profileImageUri,
                        coverImageUri = user?.coverImageUri,
                        is13Plus = user?.is13Plus ?: true
                    )
                    saveUser(updatedUser, this)
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                    onResume()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening edit dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showThemePickerDialog() {
        val themes = arrayOf("Light", "Dark", "Blue", "Green", "Purple", "Custom Image")
        AlertDialog.Builder(this)
            .setTitle("Select Profile Theme")
            .setItems(themes) { _, which ->
                try {
                    val root = findViewById<ScrollView>(R.id.scrollViewRoot)
                    when (themes[which]) {
                        "Light" -> root.setBackgroundColor(Color.parseColor("#FAFAFA"))
                        "Dark" -> root.setBackgroundColor(Color.parseColor("#212121"))
                        "Blue" -> root.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_200))
                        "Green" -> root.setBackgroundColor(ContextCompat.getColor(this, R.color.green_200))
                        "Purple" -> root.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200))
                        "Custom Image" -> pickCustomBackgroundLauncher.launch("image/*")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error applying theme: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun openSocialLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun profileSafeLoad(uri: Uri, target: ImageView) {
        try {
            Picasso.get().load(uri).fit().centerCrop().into(target)
        } catch (e: Exception) {
            try {
                target.setImageURI(uri)
            } catch (_: Exception) {}
        }
    }

    private fun remoteSafeLoad(url: String, target: ImageView) {
        try {
            Picasso.get().load(url).fit().centerCrop().into(target)
        } catch (_: Exception) {}
    }

    private fun saveUser(user: User, context: Context) {
        try {
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("username", user.username)
                putString("email", user.email)
                putString("gender", user.gender)
                putString("dob", user.dateOfBirth)
                putStringSet("games", user.favoriteGames.toSet())
                putString("profileUri", user.profileImageUri?.toString())
                putString("coverUri", user.coverImageUri?.toString())
                putBoolean("is13Plus", user.is13Plus)
                apply()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving user: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImage(uri: Uri) {
        try {
            val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("profileUri", uri.toString()).apply()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUser(context: Context): User? {
        return try {
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val username = prefs.getString("username", null) ?: return null
            val email = prefs.getString("email", "") ?: ""
            val gender = prefs.getString("gender", "") ?: ""
            val dob = prefs.getString("dob", "") ?: ""
            val games = prefs.getStringSet("games", emptySet())?.toList() ?: emptyList()
            val profileUri = prefs.getString("profileUri", null)?.let { Uri.parse(it) }
            val coverUri = prefs.getString("coverUri", null)?.let { Uri.parse(it) }
            val is13Plus = prefs.getBoolean("is13Plus", false)
            User(
                id = 1,
                username = username,
                email = email,
                gender = gender,
                dateOfBirth = dob,
                favoriteGames = games,
                profileImageUri = profileUri,
                coverImageUri = coverUri,
                is13Plus = is13Plus
            )
        } catch (e: Exception) {
            null
        }
    }
}

