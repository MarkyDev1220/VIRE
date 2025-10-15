package com.example.vire.android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vire.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get login UI components
        val loginSubmitButton = findViewById<Button>(R.id.loginSubmitButton)
        val emailOrUsernameEditText = findViewById<EditText>(R.id.emailOrUsernameEditText)
        val loginPasswordEditText = findViewById<EditText>(R.id.loginPasswordEditText)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)

        // Handle login click
        loginSubmitButton.setOnClickListener {
            val emailOrUsername = emailOrUsernameEditText.text.toString().trim()
            val password = loginPasswordEditText.text.toString().trim()

            if (emailOrUsername.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                // You can add real validation here later
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
            }
        }

        // Sign up button
        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // Forgot password text
        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}

