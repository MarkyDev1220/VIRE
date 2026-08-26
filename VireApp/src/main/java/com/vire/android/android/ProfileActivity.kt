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
import java.util.*
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

    private lateinit var socialDiscord: ImageButton
    private lateinit var socialTiktok: ImageButton
    private lateinit var socialInstagram: ImageButton
    private lateinit var addFriendButton: Button
    private lateinit var profileFeedListView: ListView
    private lateinit var profileFeedAdapter: ArrayAdapter<String>
    private val profileFeedPosts = mutableListOf<String>()

    private var changingCoverPhoto = false
    private lateinit var editPenButton: ImageButton

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (changingCoverPhoto) coverPhoto.setImageURI(uri)
            else profileImage.setImageURI(uri)
            saveProfileImage(uri)
        }
    }

    private val pickCustomBackgroundLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val root = findViewById<ScrollView>(R.id.scrollViewRoot)
            root.background = null
            val drawable: Drawable? =
                Drawable.createFromStream(contentResolver.openInputStream(uri), uri.toString())
            root.background = drawable
            Toast.makeText(this, "Custom theme applied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize views
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

        // Change profile pic
        changeProfilePicButton.setOnClickListener {
            changingCoverPhoto = false
            pickImageLauncher.launch("image/*")
        }

        // Change cover photo
        changeCoverButton.setOnClickListener {
            changingCoverPhoto = true
            pickImageLauncher.launch("image/*")
        }

        // Edit profile
        editPenButton.setOnClickListener { showEditProfileDialog() }

        // Social links
        socialDiscord.setOnClickListener { openSocialLink("https://discord.com/users/123456") }
        socialTiktok.setOnClickListener { openSocialLink("https://www.tiktok.com/@example") }
        socialInstagram.setOnClickListener { openSocialLink("https://instagram.com/example") }

        // Hamburger menu
        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener {
            val popup = PopupMenu(this, it)
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
    }

    override fun onResume() {
        super.onResume()

        val loggedInUser = loadUser(this)
        val profileUsername = intent.getStringExtra("username") ?: loggedInUser?.username ?: ""

        // Set Add Friend visibility
        addFriendButton.visibility =
            if (loggedInUser?.username.equals(profileUsername, ignoreCase = true)) Button.GONE
            else Button.VISIBLE

        // Load profile info
        val user = loggedInUser
        if (user != null) {
            usernameText.text = profileUsername
            emailText.text = user.email
            genderText.text = "Gender: ${user.gender}"
            gamesText.text = "Games: ${user.favoriteGames.joinToString(", ")}"
            user.profileImageUri?.let { profileImage.setImageURI(it) }
        }

        // Refresh feed
        profileFeedPosts.clear()
        profileFeedPosts.addAll(
            FeedManager.getProfileFeed(profileUsername)
                .map { "${it.username}: ${it.content}" }
        )
        profileFeedAdapter.notifyDataSetChanged()
    }

    private fun showEditProfileDialog() {
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
                    is13Plus = user?.is13Plus ?: true
                )
                saveUser(updatedUser, this)
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                onResume()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showThemePickerDialog() {
        val themes = arrayOf("Light", "Dark", "Blue", "Green", "Purple", "Custom Image")
        AlertDialog.Builder(this)
            .setTitle("Select Profile Theme")
            .setItems(themes) { _, which ->
                val root = findViewById<ScrollView>(R.id.scrollViewRoot)
                when (themes[which]) {
                    "Light" -> root.setBackgroundColor(Color.parseColor("#FAFAFA"))
                    "Dark" -> root.setBackgroundColor(Color.parseColor("#212121"))
                    "Blue" -> root.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_200))
                    "Green" -> root.setBackgroundColor(ContextCompat.getColor(this, R.color.green_200))
                    "Purple" -> root.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200))
                    "Custom Image" -> pickCustomBackgroundLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun openSocialLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }

    /** --- SharedPreferences --- */
    private fun saveUser(user: User, context: Context) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("username", user.username)
            putString("email", user.email)
            putString("gender", user.gender)
            putString("dob", user.dateOfBirth)
            putStringSet("games", user.favoriteGames.toSet())
            putString("profileUri", user.profileImageUri?.toString())
            putBoolean("is13Plus", user.is13Plus)
            apply()
        }
    }

    private fun saveProfileImage(uri: Uri) {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("profileUri", uri.toString()).apply()
    }

    private fun loadUser(context: Context): User? {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", null) ?: return null
        val email = prefs.getString("email", "") ?: ""
        val gender = prefs.getString("gender", "") ?: ""
        val dob = prefs.getString("dob", "") ?: ""
        val games = prefs.getStringSet("games", emptySet())?.toList() ?: emptyList()
        val profileUri = prefs.getString("profileUri", null)?.let { Uri.parse(it) }
        val is13Plus = prefs.getBoolean("is13Plus", false)
        return User(
            id = 1,
            username = username,
            email = email,
            gender = gender,
            dateOfBirth = dob,
            favoriteGames = games,
            profileImageUri = profileUri,
            is13Plus = is13Plus
        )
    }
}

