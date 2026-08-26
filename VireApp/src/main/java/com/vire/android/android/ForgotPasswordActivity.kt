package com.vire.android.android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vire.android.R

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val submitButton = findViewById<Button>(R.id.submitForgotButton)
        val resetButton = findViewById<Button>(R.id.resetPasswordButton)
        val emailField = findViewById<EditText>(R.id.forgotEmailEditText)
        val newPasswordField = findViewById<EditText>(R.id.newPasswordEditText)
        val confirmPasswordField = findViewById<EditText>(R.id.confirmNewPasswordEditText)

        submitButton.setOnClickListener {
            val userEmail = emailField.text.toString()
            if (userEmail.isBlank()) {
                Toast.makeText(this, "Please enter your email/username", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Verification link sent", Toast.LENGTH_SHORT).show()
            }
        }

        resetButton.setOnClickListener {
            val newPassword = newPasswordField.text.toString()
            val confirmPassword = confirmPasswordField.text.toString()

            if (newPassword.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(this, "Enter new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Password Reset Successful", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
