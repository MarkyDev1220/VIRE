package com.example.vire.android

import android.app.AlertDialog
import android.app.DatePickerDialog
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

class ProfileActivity : BaseActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var coverPhoto: ImageView
    private lateinit var changeProfilePicButton: ImageButton
    private lateinit var changeCoverButton: ImageButton
    private lateinit var editThemeButton: Button
    private lateinit var logoutButton: Button
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var aboutMeText: TextView
    private lateinit var gamesText: TextView
    private lateinit var editProfileButton: Button
    private lateinit var genderText: TextView

    // Social icons
    private lateinit var socialDiscord: ImageButton
    private lateinit var socialTiktok: ImageButton
    private lateinit var socialInstagram: ImageButton

    // Profile feed
    private lateinit var profileFeedListView: ListView
    private lateinit var profileFeedAdapter: ArrayAdapter<String>
    private val profileFeedPosts = mutableListOf<String>()

    private var changingCoverPhoto = false

    // Pick image for profile or cover
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (changingCoverPhoto) coverPhoto.setImageURI(uri)
            else profileImage.setImageURI(uri)
        }
    }

    // Pick custom theme image
    private val pickCustomBackgroundLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val root = findViewById<ScrollView>(R.id.scrollViewRoot)
            root.background = null
            val drawable: Drawable? = Drawable.createFromStream(contentResolver.openInputStream(uri), uri.toString())
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
        editThemeButton = findViewById(R.id.editThemeButton)
        logoutButton = findViewById(R.id.logoutButton)
        usernameText = findViewById(R.id.profileUsername)
        emailText = findViewById(R.id.profileEmail)
        aboutMeText = findViewById(R.id.profileAboutMe)
        gamesText = findViewById(R.id.profileGames)
        editProfileButton = findViewById(R.id.editProfileButton)
        genderText = findViewById(R.id.profileGender)

        socialDiscord = findViewById(R.id.socialDiscord)
        socialTiktok = findViewById(R.id.socialTiktok)
        socialInstagram = findViewById(R.id.socialInstagram)

        // Profile feed ListView
        profileFeedListView = findViewById(R.id.profileFeedListView)
        profileFeedAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, profileFeedPosts)
        profileFeedListView.adapter = profileFeedAdapter

        // Set profile info
        val email = intent.getStringExtra("email") ?: "email"
        val username = intent.getStringExtra("username") ?: "User"
        usernameText.text = username
        emailText.text = email

        aboutMeText.text = "About Me: Add a description..."
        gamesText.text = "Games: Add your favorite TCG games..."
        genderText.text = "Gender: Not set"

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

        // Edit theme button
        editThemeButton.setOnClickListener { showThemePickerDialog() }

        // Logout
        logoutButton.setOnClickListener {
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Edit profile button
        editProfileButton.setOnClickListener { showEditProfileDialog() }

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

            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                    "Profile" -> { /* Already here */ }
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
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile feed from FeedManager
        profileFeedPosts.clear()
        profileFeedPosts.addAll(
            FeedManager.getProfileFeed(usernameText.text.toString()).map { "${it.username}: ${it.content}" }
        )
        profileFeedAdapter.notifyDataSetChanged()
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

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val usernameInput = dialogView.findViewById<EditText>(R.id.editUsername)
        val emailInput = dialogView.findViewById<EditText>(R.id.editEmail)
        val aboutMeInput = dialogView.findViewById<EditText>(R.id.editAboutMe)
        val gamesInput = dialogView.findViewById<EditText>(R.id.editGames)
        val dobInput = dialogView.findViewById<EditText>(R.id.editDOB)

        usernameInput.setText(usernameText.text.toString())
        emailInput.setText(emailText.text.toString())
        aboutMeInput.setText(aboutMeText.text.toString().replace("About Me: ", ""))
        gamesInput.setText(gamesText.text.toString().replace("Games: ", ""))

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

        val tcgGames = arrayOf(
            "Magic: The Gathering",
            "Pokemon TCG",
            "Yu-Gi-Oh!",
            "Battle Spirits Saga (BSS)",
            "Cardfight Vanguard (CFV)",
            "Lorcana",
            "Force of Will (FOW)"
        )
        val selectedGames = mutableListOf<String>()
        val existingGames = gamesInput.text.toString().split(", ").filter { it.isNotBlank() }
        selectedGames.addAll(existingGames)

        gamesInput.setOnClickListener {
            val checkedItems = BooleanArray(tcgGames.size) { selectedGames.contains(tcgGames[it]) }
            AlertDialog.Builder(this)
                .setTitle("Select TCG Games")
                .setMultiChoiceItems(tcgGames, checkedItems) { _, which, isChecked ->
                    if (isChecked) selectedGames.add(tcgGames[which])
                    else selectedGames.remove(tcgGames[which])
                }
                .setPositiveButton("OK") { _, _ -> gamesInput.setText(selectedGames.joinToString(", ")) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                usernameText.text = usernameInput.text.toString()
                emailText.text = emailInput.text.toString()
                aboutMeText.text = "About Me: ${aboutMeInput.text}"
                gamesText.text = "Games: ${gamesInput.text}"
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openSocialLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }
}
