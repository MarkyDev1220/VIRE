package com.example.vire.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vire.android.databinding.ActivitySignupBinding
import com.example.vire.android.ProfileActivity


class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private var profileImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Populate the Clan Leader/Nation Leader dropdown with Yes/No options
        val options = arrayOf("No", "Yes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.clanLeaderSpinner.adapter = adapter

        binding.profileImageView.setOnClickListener {
            openImagePicker()
        }

        binding.signupButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val dob = binding.dobEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            val discordUsername = binding.discordUsernameEditText.text.toString().trim()
            val clanLeader = binding.clanLeaderSpinner.selectedItem.toString()
            val socialsLink = binding.socialsLinkEditText.text.toString().trim()

            // Validate mandatory fields
            if (name.isEmpty() || dob.isEmpty() || email.isEmpty() ||
                username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
                discordUsername.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all mandatory fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Check if username already exists in backend/local DB
            if (fakeUsernameExists(username)) {
                Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Save user data including profileImageUri

            Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()

            // Navigate to Profile or Home screen
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            profileImageUri = data?.data
            binding.profileImageView.setImageURI(profileImageUri)
        }
    }

    private fun fakeUsernameExists(username: String): Boolean {
        // Placeholder for username uniqueness check; replace with real backend call
        val takenUsernames = listOf("user1", "admin", "testuser")
        return takenUsernames.contains(username.lowercase())
    }
}

