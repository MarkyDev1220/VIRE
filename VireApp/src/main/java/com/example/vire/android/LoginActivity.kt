package com.example.vire.android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginSubmitButton = findViewById<Button>(R.id.loginSubmitButton)
        val emailOrUsernameEditText = findViewById<EditText>(R.id.emailOrUsernameEditText)
        val loginPasswordEditText = findViewById<EditText>(R.id.loginPasswordEditText)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)

        loginSubmitButton.setOnClickListener {
            val emailOrUsername = emailOrUsernameEditText.text.toString()
            val password = loginPasswordEditText.text.toString()

            if (emailOrUsername.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Proceed to home/profile screen
                startActivity(Intent(this, HomeActivity::class.java))
            }
        }

        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}

