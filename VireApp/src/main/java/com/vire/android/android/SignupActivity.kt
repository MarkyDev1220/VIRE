package com.vire.android.android

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.vire.android.databinding.ActivitySignupBinding
import java.util.*

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private var profileImageUri: Uri? = null
    private val selectedGames = mutableListOf<String>()
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val genderOptions = arrayOf(
        "Male","Female","Non-binary","Transgender Male","Transgender Female","Other / Prefer not to say"
    )
    private val tcgGames = arrayOf(
        "Magic: The Gathering","Pokemon TCG","Yu-Gi-Oh!","Battle Spirits Saga (BSS)",
        "Cardfight Vanguard (CFV)","Lorcana","Force of Will (FOW)"
    )

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            binding.profileImageView.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.genderSpinner.adapter = genderAdapter

        binding.profileImageView.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.editDOB.inputType = InputType.TYPE_NULL
        binding.editDOB.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d -> binding.editDOB.setText("${m+1}/$d/$y") },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.gamesSelect.setOnClickListener {
            val checkedItems = BooleanArray(tcgGames.size) { selectedGames.contains(tcgGames[it]) }
            AlertDialog.Builder(this)
                .setTitle("Select TCG Games")
                .setMultiChoiceItems(tcgGames, checkedItems) { _, which, isChecked ->
                    if (isChecked) selectedGames.add(tcgGames[which]) else selectedGames.remove(tcgGames[which])
                }
                .setPositiveButton("OK") { _, _ ->
                    binding.gamesSelect.setText(if (selectedGames.isNotEmpty()) selectedGames.joinToString(", ") else "Select Games")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.returnText.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.signupButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val username = binding.usernameEditText.text.toString().trim()
            val firstName = binding.firstNameEditText.text.toString().trim()
            val lastName = binding.lastNameEditText.text.toString().trim()
            val fullName = "$firstName $lastName".trim()
            val createPassword = binding.createpasswordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            val gender = binding.genderSpinner.selectedItem.toString()
            val dobText = binding.editDOB.text.toString()
            val is13Plus = binding.check13Plus.isChecked

            if (email.isEmpty() || username.isEmpty() || createPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEditText.error = "Enter a valid email"
                return@setOnClickListener
            }

            if (username.length !in 3..64) {
                binding.usernameEditText.error = "Username must be between 3 and 64 characters"
                return@setOnClickListener
            }

            if (createPassword.length < 6) {
                binding.createpasswordEditText.error = "Password must be at least 6 characters"
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (createPassword != confirmPassword) {
                binding.confirmPasswordEditText.error = "Passwords do not match"
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!is13Plus) {
                Toast.makeText(this, "You must be 13 or older to sign up", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val signedInUser = auth.currentUser?.takeIf {
                it.email.equals(email, ignoreCase = true)
            }

            if (signedInUser != null) {
                persistSignupProfile(
                    signedInUser.uid, email, username, firstName, lastName,
                    fullName, gender, dobText, is13Plus
                )
            } else {
                auth.createUserWithEmailAndPassword(email, createPassword)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid
                        if (uid == null) {
                            Toast.makeText(this, "Account was created, but its user ID is unavailable.", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }
                        persistSignupProfile(
                            uid, email, username, firstName, lastName,
                            fullName, gender, dobText, is13Plus
                        )
                    }
                    .addOnFailureListener { e ->
                        val errorMsg = if (e is SecurityException ||
                            e.message?.contains("broker", ignoreCase = true) == true ||
                            e.message?.contains("Unknown calling package", ignoreCase = true) == true) {
                            "Google Play Services could not complete sign-up: ${e.localizedMessage}"
                        } else if (e.message?.contains("network", ignoreCase = true) == true ||
                            e.message?.contains("unreachable host", ignoreCase = true) == true) {
                            "Network error: Unable to reach Firebase. Check your connection and try again."
                        } else {
                            "Signup failed: ${e.localizedMessage}"
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    private fun persistSignupProfile(
        uid: String,
        email: String,
        username: String,
        firstName: String,
        lastName: String,
        fullName: String,
        gender: String,
        dobText: String,
        is13Plus: Boolean
    ) {
        val userMap = hashMapOf(
            "uid" to uid,
            "username" to username,
            "usernameLowercase" to username.lowercase(),
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "fullName" to fullName,
            "gender" to gender,
            "dateOfBirth" to dobText,
            "favoriteGames" to selectedGames.toList(),
            "favoriteGenres" to emptyList<String>(),
            "skillLevel" to "",
            "localArea" to "",
            "gamerBio" to "",
            "is13Plus" to is13Plus,
            "profileImageUrl" to "",
            "coverImageUrl" to "",
            "aboutMe" to ""
        )

        val user = User(
            id = uid,
            username = username,
            email = email,
            firstName = firstName,
            lastName = lastName,
            fullName = fullName,
            gender = gender,
            dateOfBirth = dobText,
            favoriteGames = selectedGames.toList(),
            is13Plus = is13Plus
        )

        db.collection("users").document(uid).set(userMap)
            .continueWithTask { privateWrite ->
                if (!privateWrite.isSuccessful) {
                    throw privateWrite.exception ?: IllegalStateException("Private profile write failed")
                }
                db.collection(UserDirectory.COLLECTION)
                    .document(uid)
                    .set(UserDirectory.fromUser(user))
            }
            .addOnSuccessListener {
                saveUser(user, this)
                UserManager.addUser(user)

                val imageUri = profileImageUri
                if (imageUri == null) {
                    continueToProfile(uid, username)
                } else {
                    uploadProfileImage(uid, imageUri) { profileUrl ->
                        db.collection("users").document(uid)
                            .update("profileImageUrl", profileUrl)
                            .continueWithTask { privateImageWrite ->
                                if (!privateImageWrite.isSuccessful) {
                                    throw privateImageWrite.exception
                                        ?: IllegalStateException("Private image write failed")
                                }
                                db.collection(UserDirectory.COLLECTION)
                                    .document(uid)
                                    .update("profileImageUrl", profileUrl)
                            }
                            .addOnCompleteListener {
                                continueToProfile(uid, username)
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                val message = if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
                    e.message?.contains("Missing or insufficient permissions", ignoreCase = true) == true) {
                    "Firestore denied saving your profile. Check that the current user's private and public profile rules are deployed."
                } else {
                    "Your account was created, but the profile could not be saved: ${e.localizedMessage}"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
    }

    private fun continueToProfile(uid: String, username: String) {
        Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("uid", uid)
        intent.putExtra("username", username)
        startActivity(intent)
        finish()
    }

    private fun uploadProfileImage(uid: String, uri: Uri, onComplete: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("profileImages/$uid/profileImageUrl.jpg")

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->
                onComplete(downloadUrl.toString())
            }
            .addOnFailureListener {
                Toast.makeText(this, "Profile image upload failed. You can add it later.", Toast.LENGTH_SHORT).show()
                onComplete("")
            }
    }
}
