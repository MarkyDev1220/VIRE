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

            if (username.length < 3) {
                binding.usernameEditText.error = "Username must be at least 3 characters"
                return@setOnClickListener
            }

            if (createPassword.length < 8) {
                binding.createpasswordEditText.error = "Password must be at least 8 characters"
                return@setOnClickListener
            }

            if (createPassword != confirmPassword) {
                binding.confirmPasswordEditText.error = "Passwords do not match"
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!is13Plus) {
                Toast.makeText(this, "You must be 13 or older", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, createPassword)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    val userMap = hashMapOf(
                        "uid" to uid,
                        "username" to username,
                        "usernameLowercase" to username.lowercase(),
                        "email" to email,
                        "gender" to gender,
                        "dateOfBirth" to dobText,
                        "favoriteGames" to selectedGames,
                        "favoriteGenres" to emptyList<String>(),
                        "skillLevel" to "",
                        "localArea" to "",
                        "gamerBio" to "",
                        "is13Plus" to is13Plus,
                        "profileImageUrl" to "",
                        "coverImageUrl" to "",
                        "aboutMe" to ""
                    )

                    val userObj = User(
                        id = uid,
                        username = username,
                        email = email,
                        gender = gender,
                        dateOfBirth = dobText,
                        favoriteGames = selectedGames,
                        is13Plus = is13Plus
                    )

                    saveUser(userObj, this)

                    db.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, ProfileActivity::class.java)
                            intent.putExtra("uid", uid)
                            intent.putExtra("username", username)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Signup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
