package com.example.vire.android

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
import com.example.vire.android.databinding.ActivitySignupBinding
import java.util.*

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private var profileImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            binding.profileImageView.setImageURI(uri)
        }
    }

    private val genderOptions = arrayOf(
        "Male",
        "Female",
        "Non-binary",
        "Transgender Male",
        "Transgender Female",
        "Other / Prefer not to say"
    )

    // TCG games and selection
    private val tcgGames = arrayOf(
        "Magic: The Gathering",
        "Pokemon TCG",
        "Yu-Gi-Oh!",
        "Battle Spirits Saga (BSS)",
        "Cardfight Vanguard (CFV)",
        "Lorcana",
        "Force of Will (FOW)"
    )
    private val selectedGames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup gender spinner
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.genderSpinner.adapter = genderAdapter

        // Profile image picker
        binding.profileImageView.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // DOB picker
        binding.editDOB.inputType = InputType.TYPE_NULL
        binding.editDOB.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    binding.editDOB.setText("${month + 1}/$dayOfMonth/$year")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // TCG games selection
        binding.gamesSelect.setOnClickListener {
            val checkedItems = BooleanArray(tcgGames.size) { selectedGames.contains(tcgGames[it]) }
            AlertDialog.Builder(this)
                .setTitle("Select TCG Games")
                .setMultiChoiceItems(tcgGames, checkedItems) { _, which, isChecked ->
                    if (isChecked) {
                        if (!selectedGames.contains(tcgGames[which])) selectedGames.add(tcgGames[which])
                    } else {
                        selectedGames.remove(tcgGames[which])
                    }
                }
                .setPositiveButton("OK") { _, _ ->
                    binding.gamesSelect.setText(
                        if (selectedGames.isNotEmpty()) selectedGames.joinToString(", ") else "Select Games"
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Return button to go back to MainActivity
        binding.returnText.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
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

            // Validation
            if (email.isEmpty() || username.isEmpty() || createPassword.isEmpty() ||
                confirmPassword.isEmpty() || dobText.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all mandatory fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!is13Plus) {
                Toast.makeText(this, "You must confirm that you are 13 years or older", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (createPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (fakeUsernameExists(username)) {
                Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Optional: Validate age is actually 13+
            val parts = dobText.split("/")
            if (parts.size == 3) {
                val birthYear = parts[2].toInt()
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                if (currentYear - birthYear < 13) {
                    Toast.makeText(this, "You must be at least 13 years old to sign up", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if (selectedGames.isEmpty()) {
                Toast.makeText(this, "Please select at least one TCG game", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Save user data including profileImageUri, gender, DOB, and selectedGames

            Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("username", username)
            intent.putExtra("email", email)
            intent.putExtra("gender", gender)
            intent.putStringArrayListExtra("selectedGames", ArrayList(selectedGames))
            startActivity(intent)
            finish()
        }
    }

    private fun fakeUsernameExists(username: String): Boolean {
        val takenUsernames = listOf("user1", "admin", "testuser")
        return takenUsernames.contains(username.lowercase())
    }
}
