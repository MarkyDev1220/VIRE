package com.vire.android.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isLogout = intent.getBooleanExtra("is_logout", false)

        // Check if user is already logged in
        val currentUser = auth.currentUser
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedUid = prefs.getString("uid", null)

        if (!isLogout && (currentUser != null || savedUid != null)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        // Handle login click
        binding.loginSubmitButton.setOnClickListener {
            val emailOrUsername = binding.emailOrUsernameEditText.text.toString().trim()
            val password = binding.loginPasswordEditText.text.toString().trim()

            if (emailOrUsername.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (emailOrUsername.contains("@")) {
                performFirebaseLogin(emailOrUsername, password)
            } else {
                // Lookup email by case-insensitive username in Firestore
                db.collection("users").get()
                    .addOnSuccessListener { result ->
                        val matchedDoc = result.documents.find { doc ->
                            val u = doc.getString("username")
                            u != null && u.equals(emailOrUsername, ignoreCase = true)
                        }

                        val email = matchedDoc?.getString("email")
                        if (!email.isNullOrEmpty()) {
                            performFirebaseLogin(email, password)
                        } else {
                            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Sign up button
        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // Forgot password text
        binding.forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun performFirebaseLogin(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val username = doc.getString("username") ?: email.substringBefore("@")
                        saveLocalSession(uid, username, email)
                        Toast.makeText(this, "Welcome back, $username!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        saveLocalSession(uid, email.substringBefore("@"), email)
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveLocalSession(uid: String, username: String, email: String = "") {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("uid", uid)
            .putString("username", username)
            .putString("email", email)
            .apply()
    }
}
