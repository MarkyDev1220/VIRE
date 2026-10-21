package com.vire.android.android

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import com.vire.android.R
import java.io.ByteArrayOutputStream
import java.util.*

class ProfileActivity : BaseActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var coverPhoto: ImageView
    private lateinit var changeProfilePicButton: ImageButton
    private lateinit var changeCoverButton: ImageButton
    private lateinit var fullNameText: TextView
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var aboutMeText: TextView
    private lateinit var gamesText: TextView
    private lateinit var genderText: TextView

    private lateinit var genresText: TextView
    private lateinit var skillLevelText: TextView
    private lateinit var localAreaText: TextView
    private lateinit var gamerBioText: TextView

    private lateinit var socialDiscord: ImageButton
    private lateinit var socialTiktok: ImageButton
    private lateinit var socialInstagram: ImageButton
    private lateinit var addFriendButton: Button
    private lateinit var profileFeedListView: ListView
    private lateinit var profileFeedAdapter: ArrayAdapter<String>
    private val profileFeedPosts = mutableListOf<String>()

    private lateinit var hostedGameNightsListView: ListView
    private lateinit var hostedGameNightsAdapter: ArrayAdapter<String>
    private val hostedGameNightsList = mutableListOf<GameNight>()

    private lateinit var editPenButton: ImageButton

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
        fullNameText = findViewById(R.id.profileFullName)
        usernameText = findViewById(R.id.profileUsername)
        emailText = findViewById(R.id.profileEmail)
        aboutMeText = findViewById(R.id.profileAboutMe)
        gamesText = findViewById(R.id.profileGames)
        genderText = findViewById(R.id.profileGender)

        genresText = findViewById(R.id.profileGenres)
        skillLevelText = findViewById(R.id.profileSkillLevel)
        localAreaText = findViewById(R.id.profileLocalArea)
        gamerBioText = findViewById(R.id.profileGamerBio)

        socialDiscord = findViewById(R.id.socialDiscord)
        socialTiktok = findViewById(R.id.socialTiktok)
        socialInstagram = findViewById(R.id.socialInstagram)
        addFriendButton = findViewById(R.id.addFriendButton)
        editPenButton = findViewById(R.id.editPenButton)

        profileFeedListView = findViewById(R.id.profileFeedListView)
        profileFeedAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, profileFeedPosts)
        profileFeedListView.adapter = profileFeedAdapter

        hostedGameNightsListView = findViewById(R.id.hostedGameNightsListView)
        hostedGameNightsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        hostedGameNightsListView.adapter = hostedGameNightsAdapter

        hostedGameNightsListView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, GameNightDetailsActivity::class.java)
            intent.putExtra("game_night", hostedGameNightsList[position])
            startActivity(intent)
        }

        findViewById<Button>(R.id.viewCollectionButton)?.setOnClickListener {
            startActivity(Intent(this, CollectionActivity::class.java))
        }

        setupHamburgerMenu()
        emailText.visibility = View.GONE

        val auth = try {
            FirebaseAuth.getInstance()
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "Firebase not initialized. Please check your configuration.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val explicitUid = intent.getStringExtra("uid")
        val authUser = auth.currentUser
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedUid = prefs.getString("uid", null)

        val loggedInUid = authUser?.uid ?: savedUid
        val uid = explicitUid ?: loggedInUid

        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in. Please sign in again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentUid = uid

        emailText.visibility = View.GONE

        val isSelf = (uid == loggedInUid)
        if (isSelf) {
            addFriendButton.visibility = View.GONE
            editPenButton.visibility = View.VISIBLE
            changeProfilePicButton.visibility = View.VISIBLE
            changeCoverButton.visibility = View.VISIBLE
        } else {
            addFriendButton.visibility = View.VISIBLE
            editPenButton.visibility = View.GONE
            changeProfilePicButton.visibility = View.GONE
            changeCoverButton.visibility = View.GONE
        }

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

        socialDiscord.setOnClickListener { openSocialLink("https://discord.com") }
        socialTiktok.setOnClickListener { openSocialLink("https://tiktok.com") }
        socialInstagram.setOnClickListener { openSocialLink("https://instagram.com") }
        findViewById<ImageButton>(R.id.socialFacebook)?.setOnClickListener { openSocialLink("https://facebook.com") }
        findViewById<ImageButton>(R.id.socialtwitch)?.setOnClickListener { openSocialLink("https://twitch.tv") }
        findViewById<ImageButton>(R.id.socialkick)?.setOnClickListener { openSocialLink("https://kick.com") }

        addFriendButton.setOnClickListener {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val profileUid = intent.getStringExtra("uid") ?: return@setOnClickListener

            if (currentUid == null) {
                Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentUid == profileUid) {
                Toast.makeText(this, "You cannot add yourself.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FriendManager.isFriend(profileUid) { isFriend ->
                if (isFriend) {
                    Toast.makeText(this, "Already friends.", Toast.LENGTH_SHORT).show()
                } else {
                    FriendManager.sendRequest(profileUid) { success ->
                        Toast.makeText(
                            this,
                            if (success) "Friend request sent!" else "Failed to send request.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        currentUid?.let { loadUserProfile(it) }
    }

    private fun applyThemeColor(themeName: String) {
        val root = findViewById<ScrollView>(R.id.scrollViewRoot) ?: return
        when (themeName) {
            "Light" -> root.setBackgroundColor(Color.parseColor("#FAFAFA"))
            "Dark" -> root.setBackgroundColor(Color.parseColor("#111625"))
            "Blue" -> root.setBackgroundColor(Color.parseColor("#0F172A"))
            "Green" -> root.setBackgroundColor(Color.parseColor("#0A1F1C"))
            "Purple" -> root.setBackgroundColor(Color.parseColor("#180E29"))
            else -> root.setBackgroundColor(Color.parseColor("#111625"))
        }
    }

    private fun loadUserProfile(uid: String) {
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "Firebase not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val username = doc.getString("username") ?: "Unknown"
                    usernameText.text = "Username: $username"

                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val rawFullName = doc.getString("fullName")
                    val fullName = when {
                        !rawFullName.isNullOrBlank() -> rawFullName
                        firstName.isNotBlank() || lastName.isNotBlank() -> "$firstName $lastName".trim()
                        else -> doc.getString("name") ?: doc.getString("displayName") ?: username
                    }
                    fullNameText.text = fullName

                    val profileTheme = doc.getString("profileTheme") ?: "Dark"
                    applyThemeColor(profileTheme)

                    val gender = doc.getString("gender") ?: ""
                    genderText.text = if (gender.isNotBlank() && gender != "Not set") "Gender: $gender" else "Gender: Not specified"

                    val aboutMe = doc.getString("aboutMe") ?: ""
                    aboutMeText.text = if (aboutMe.isNotBlank() && aboutMe != "No bio yet") "About Me: $aboutMe" else "About Me: No bio added yet"

                    val games = (doc.get("favoriteGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    gamesText.text = "Games: ${if (games.isNotEmpty()) games.joinToString(", ") else "No favorite games added yet"}"

                    val genres = (doc.get("favoriteGenres") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    genresText.text = "Favorite Genres: ${if (genres.isNotEmpty()) genres.joinToString(", ") else "No favorite genres added yet"}"

                    val skillLevel = doc.getString("skillLevel") ?: ""
                    skillLevelText.text = "Skill Level: ${if (skillLevel.isNotBlank() && skillLevel != "Not set") skillLevel else "Not specified"}"

                    val localArea = doc.getString("localArea") ?: ""
                    localAreaText.text = "Local Gaming Area: ${if (localArea.isNotBlank() && localArea != "Not set") localArea else "Not specified"}"

                    val gamerBio = doc.getString("gamerBio") ?: ""
                    gamerBioText.text = "Gamer Bio: ${if (gamerBio.isNotBlank() && gamerBio != "None") gamerBio else "No gamer bio added yet"}"

                    Log.d("ProfileActivity", "User $uid document data: ${doc.data}")

                    val profileUrl = doc.getString("profileImageUrl")
                        ?: doc.getString("profileImage")
                        ?: doc.getString("avatarUrl")
                        ?: doc.getString("profileImageUri")
                        ?: ""

                    val coverUrl = doc.getString("coverImageUrl")
                        ?: doc.getString("coverImage")
                        ?: doc.getString("coverUrl")
                        ?: doc.getString("coverImageUri")
                        ?: ""

                    val isMychaela = username.contains("Mychaela", ignoreCase = true) ||
                            fullName.contains("Mychaela", ignoreCase = true) ||
                            username.contains("Thomas", ignoreCase = true)

                    val defaultAvatar = if (isMychaela) R.drawable.mychaela_avatar else R.drawable.ic_profile_placeholder
                    val defaultCover = if (isMychaela) R.drawable.mychaela_cover else R.drawable.default_cover

                    if (profileUrl.isNotBlank() && !profileUrl.startsWith("content://")) {
                        remoteSafeLoad(profileUrl, profileImage)
                    } else {
                        profileImage.setImageResource(defaultAvatar)
                        try {
                            FirebaseStorage.getInstance().reference
                                .child("profileImages/$uid/profileImageUrl.jpg")
                                .downloadUrl
                                .addOnSuccessListener { downloadUrl ->
                                    remoteSafeLoad(downloadUrl.toString(), profileImage)
                                }
                        } catch (_: Exception) {}
                    }

                    if (coverUrl.isNotBlank() && !coverUrl.startsWith("content://")) {
                        remoteSafeLoad(coverUrl, coverPhoto)
                    } else {
                        coverPhoto.setImageResource(defaultCover)
                        try {
                            FirebaseStorage.getInstance().reference
                                .child("profileImages/$uid/coverImageUrl.jpg")
                                .downloadUrl
                                .addOnSuccessListener { downloadUrl ->
                                    remoteSafeLoad(downloadUrl.toString(), coverPhoto)
                                }
                        } catch (_: Exception) {}
                    }

                    val loggedInUid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == loggedInUid) {
                        val currentLocalUser = User(
                            id = uid,
                            username = username,
                            email = doc.getString("email") ?: "",
                            firstName = firstName,
                            lastName = lastName,
                            fullName = fullName,
                            gender = gender,
                            dateOfBirth = doc.getString("dateOfBirth") ?: "",
                            favoriteGames = games,
                            favoriteGenres = genres,
                            skillLevel = skillLevel,
                            localArea = localArea,
                            gamerBio = gamerBio,
                            aboutMe = aboutMe
                        )
                        saveUser(currentLocalUser, this)
                    }
                } else {
                    val loggedInUid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == loggedInUid) {
                        loadFromLocalUserPrefs()
                    } else {
                        fullNameText.text = "Mychaela Thomas"
                        usernameText.text = "Username: Mychaela Thomas"
                        profileImage.setImageResource(R.drawable.mychaela_avatar)
                        coverPhoto.setImageResource(R.drawable.mychaela_cover)
                    }
                }

                if (uid == FirebaseAuth.getInstance().currentUser?.uid) {
                    addFriendButton.visibility = View.GONE
                } else {
                    addFriendButton.visibility = View.VISIBLE
                }

                loadHostedGameNights(uid)
            }
            .addOnFailureListener {
                val loggedInUid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == loggedInUid) {
                    loadFromLocalUserPrefs()
                } else {
                    fullNameText.text = "Mychaela Thomas"
                    usernameText.text = "Username: Mychaela Thomas"
                    profileImage.setImageResource(R.drawable.mychaela_avatar)
                    coverPhoto.setImageResource(R.drawable.mychaela_cover)
                }
                loadHostedGameNights(uid)
            }
    }

    private fun loadFromLocalUserPrefs() {
        val u = loadUser(this) ?: return
        fullNameText.text = u.fullName.ifBlank { u.username }
        usernameText.text = "Username: ${u.username}"
        emailText.text = "Email: ${u.email}"
        emailText.visibility = View.GONE
        genderText.text = "Gender: ${u.gender.ifBlank { "Not specified" }}"
        aboutMeText.text = "About Me: ${u.aboutMe.ifBlank { "No bio added yet" }}"
        gamesText.text = "Games: ${if (u.favoriteGames.isNotEmpty()) u.favoriteGames.joinToString(", ") else "No favorite games added yet"}"
        genresText.text = "Favorite Genres: ${if (u.favoriteGenres.isNotEmpty()) u.favoriteGenres.joinToString(", ") else "No favorite genres added yet"}"
        skillLevelText.text = "Skill Level: ${u.skillLevel.ifBlank { "Not specified" }}"
        localAreaText.text = "Local Gaming Area: ${u.localArea.ifBlank { "Not specified" }}"
        gamerBioText.text = "Gamer Bio: ${u.gamerBio.ifBlank { "No gamer bio added yet" }}"
        profileImage.setImageResource(R.drawable.ic_profile_placeholder)
        coverPhoto.setImageResource(R.drawable.default_cover)
    }

    private fun loadHostedGameNights(uid: String) {
        GameNightManager.getGameNights { list ->
            val filtered = list.filter { it.hostUid == uid }
            hostedGameNightsList.clear()
            hostedGameNightsList.addAll(filtered)

            val displayStrings = filtered.map { "${it.gameTitle}\n${it.date} @ ${it.time}" }
            hostedGameNightsAdapter.clear()
            hostedGameNightsAdapter.addAll(displayStrings)
            hostedGameNightsAdapter.notifyDataSetChanged()
        }
    }

    private fun showEditProfileDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)

            val firstNameInput = dialogView.findViewById<EditText>(R.id.editFirstName)
            val lastNameInput = dialogView.findViewById<EditText>(R.id.editLastName)
            val usernameInput = dialogView.findViewById<EditText>(R.id.editUsername)
            val emailInput = dialogView.findViewById<EditText>(R.id.editEmail)
            val aboutMeInput = dialogView.findViewById<EditText>(R.id.editAboutMe)
            val gamesInput = dialogView.findViewById<EditText>(R.id.editGames)
            val genresInput = dialogView.findViewById<EditText>(R.id.editGenres)
            val skillInput = dialogView.findViewById<EditText>(R.id.editSkillLevel)
            val areaInput = dialogView.findViewById<EditText>(R.id.editLocalArea)
            val gamerBioInput = dialogView.findViewById<EditText>(R.id.editGamerBio)
            val dobInput = dialogView.findViewById<EditText>(R.id.editDOB)
            val genderSpinner = dialogView.findViewById<Spinner>(R.id.editGender)
            val themeBtn = dialogView.findViewById<Button>(R.id.editThemeButtonDialog)

            val user = loadUser(this)

            user?.let {
                firstNameInput.setText(it.firstName)
                lastNameInput.setText(it.lastName)
                usernameInput.setText(it.username)
                emailInput.setText(it.email)
                aboutMeInput.setText(it.aboutMe)
                gamesInput.setText(it.favoriteGames.joinToString(", "))
                genresInput.setText(it.favoriteGenres.joinToString(", "))
                skillInput.setText(it.skillLevel)
                areaInput.setText(it.localArea)
                gamerBioInput.setText(it.gamerBio)
                dobInput.setText(it.dateOfBirth)

                val genders = arrayOf("Not specified", "Male", "Female", "Non-binary", "Other")
                val genderIdx = genders.indexOfFirst { g -> g.equals(it.gender, ignoreCase = true) }
                if (genderIdx >= 0) genderSpinner.setSelection(genderIdx)
            }

            dobInput.inputType = InputType.TYPE_NULL
            dobInput.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this,
                    { _, year, month, dayOfMonth ->
                        dobInput.setText("${month + 1}/$dayOfMonth/$year")
                    },
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
                    val newFirstName = firstNameInput.text.toString().trim()
                    val newLastName = lastNameInput.text.toString().trim()
                    val newFullName = "$newFirstName $newLastName".trim()

                    val updatedUser = User(
                        id = currentUid ?: "",
                        username = usernameInput.text.toString().trim(),
                        email = emailInput.text.toString().trim(),
                        firstName = newFirstName,
                        lastName = newLastName,
                        fullName = newFullName,
                        gender = genderSpinner.selectedItem?.toString() ?: "",
                        dateOfBirth = dobInput.text.toString().trim(),
                        favoriteGames = gamesInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        favoriteGenres = genresInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        skillLevel = skillInput.text.toString().trim(),
                        localArea = areaInput.text.toString().trim(),
                        gamerBio = gamerBioInput.text.toString().trim(),
                        aboutMe = aboutMeInput.text.toString().trim(),
                        profileImageUri = user?.profileImageUri,
                        coverImageUri = user?.coverImageUri,
                        is13Plus = user?.is13Plus ?: true
                    )
                    saveUser(updatedUser, this)
                    saveUserToFirestore(updatedUser)

                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                    currentUid?.let { loadUserProfile(it) }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening edit dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserToFirestore(user: User) {
        val uid = currentUid ?: return
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: IllegalStateException) {
            return
        }

        val data = hashMapOf(
            "username" to user.username,
            "usernameLowercase" to user.username.lowercase(),
            "email" to user.email,
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "fullName" to user.fullName,
            "gender" to user.gender,
            "dateOfBirth" to user.dateOfBirth,
            "favoriteGames" to user.favoriteGames,
            "favoriteGenres" to user.favoriteGenres,
            "skillLevel" to user.skillLevel,
            "localArea" to user.localArea,
            "gamerBio" to user.gamerBio,
            "aboutMe" to user.aboutMe
        )

        db.collection("users").document(uid)
            .set(data as Map<String, Any>, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Profile updated locally", Toast.LENGTH_SHORT).show()
            }
    }

    private fun convertUriToBase64(uri: Uri, isCover: Boolean): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            val maxDim = if (isCover) 800 else 400
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = (maxDim.toFloat() / Math.max(width, height)).coerceAtMost(1.0f)

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(originalBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val byteArray = outputStream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error converting image to Base64: ${e.message}")
            null
        }
    }

    private fun uploadImageToStorage(uid: String, uri: Uri, fieldName: String) {
        try {
            val isCover = (fieldName == "coverImageUrl")
            val base64Data = convertUriToBase64(uri, isCover)

            if (!base64Data.isNullOrEmpty()) {
                // Save Base64 string directly to Firestore so image syncs immediately across all devices
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(mapOf(fieldName to base64Data), com.google.firebase.firestore.SetOptions.merge())
            }

            val storage = try {
                FirebaseStorage.getInstance()
            } catch (e: IllegalStateException) {
                return
            }
            val storageRef = storage.reference.child("profileImages/$uid/$fieldName.jpg")

            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        try {
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(mapOf(fieldName to downloadUrl.toString()), com.google.firebase.firestore.SetOptions.merge())
                        } catch (_: IllegalStateException) {}
                    }
                }
                .addOnFailureListener {
                    Log.e("ProfileActivity", "Storage upload failed: ${it.message}")
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showThemePickerDialog() {
        val themes = arrayOf("Dark", "Light", "Blue", "Green", "Purple", "Custom Image")
        AlertDialog.Builder(this)
            .setTitle("Select Profile Theme")
            .setItems(themes) { _, which ->
                val selectedTheme = themes[which]
                try {
                    if (selectedTheme == "Custom Image") {
                        pickCustomBackgroundLauncher.launch("image/*")
                    } else {
                        applyThemeColor(selectedTheme)
                        val uid = currentUid ?: return@setItems
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .set(mapOf("profileTheme" to selectedTheme), com.google.firebase.firestore.SetOptions.merge())
                        Toast.makeText(this, "Theme saved!", Toast.LENGTH_SHORT).show()
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
        val placeholder = if (target.id == R.id.coverPhoto) R.drawable.default_cover else R.drawable.ic_profile_placeholder
        if (url.isBlank()) {
            target.setImageResource(placeholder)
            return
        }

        try {
            if (url.startsWith("data:image")) {
                val pureBase64 = url.substringAfter("base64,")
                val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                if (bitmap != null) {
                    target.setImageBitmap(bitmap)
                    return
                }
            }

            if (url.startsWith("http")) {
                Picasso.get()
                    .load(url)
                    .placeholder(placeholder)
                    .error(placeholder)
                    .fit()
                    .centerCrop()
                    .into(target)
                return
            }

            if (url.startsWith("content://") || url.startsWith("file://")) {
                val uri = Uri.parse(url)
                try {
                    val stream = contentResolver.openInputStream(uri)
                    if (stream != null) {
                        stream.close()
                        Picasso.get()
                            .load(uri)
                            .placeholder(placeholder)
                            .error(placeholder)
                            .fit()
                            .centerCrop()
                            .into(target)
                        return
                    }
                } catch (_: Exception) {
                    // content:// URI belongs to another physical device. Fallback to placeholder.
                }
            }

            target.setImageResource(placeholder)
        } catch (_: Exception) {
            target.setImageResource(placeholder)
        }
    }
}
