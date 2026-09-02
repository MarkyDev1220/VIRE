// SignupActivity.kt
package com.vire.android.android

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
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
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

        // Gender spinner
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.genderSpinner.adapter = genderAdapter

        // Profile image picker
        binding.profileImageView.setOnClickListener { pickImageLauncher.launch("image/*") }

        // DOB picker
        binding.editDOB.inputType = InputType.TYPE_NULL
        binding.editDOB.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d -> binding.editDOB.setText("${m+1}/$d/$y") },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Games multi-select
        binding.gamesSelect.setOnClickListener {
            val checkedItems = BooleanArray(tcgGames.size) { selectedGames.contains(tcgGames[it]) }
            AlertDialog.Builder(this)
                .setTitle("Select TCG Games")
                .setMultiChoiceItems(tcgGames, checkedItems) { _, which, isChecked ->
                    if(isChecked) selectedGames.add(tcgGames[which]) else selectedGames.remove(tcgGames[which])
                }
                .setPositiveButton("OK"){_,_ ->
                    binding.gamesSelect.setText(if(selectedGames.isNotEmpty()) selectedGames.joinToString(", ") else "Select Games")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Return button
        binding.returnText.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Sign-up button
        binding.signupButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val username = binding.usernameEditText.text.toString().trim()
            val createPassword = binding.createpasswordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            val gender = binding.genderSpinner.selectedItem.toString()
            val dobText = binding.editDOB.text.toString()
            val is13Plus = binding.check13Plus.isChecked

            if (email.isEmpty() || username.isEmpty() || createPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (createPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!is13Plus) {
                Toast.makeText(this, "You must be 13 or older", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create Firebase user
            auth.createUserWithEmailAndPassword(email, createPassword)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    // Save user data to Firestore
                    val user = hashMapOf(
                        "uid" to uid,
                        "username" to username,
                        "email" to email,
                        "gender" to gender,
                        "dateOfBirth" to dobText,
                        "favoriteGames" to selectedGames,
                        "is13Plus" to is13Plus,
                        "profileImageUrl" to "",
                        "coverImageUrl" to "",
                        "aboutMe" to ""
                    )

                    db.collection("users").document(uid).set(user)
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
