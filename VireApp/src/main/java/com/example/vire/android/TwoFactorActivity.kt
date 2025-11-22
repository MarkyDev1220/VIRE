package com.example.vire.android

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TwoFactorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two_factor)

        val enableButton = findViewById<Button>(R.id.enable2FAButton)
        enableButton.setOnClickListener {
            Toast.makeText(this, "Two-Factor Authentication Enabled", Toast.LENGTH_SHORT).show()
        }
    }
}
