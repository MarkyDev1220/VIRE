package com.example.vire.android

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val email = intent.getStringExtra("email")
        val username = intent.getStringExtra("username")
        val socials = intent.getStringExtra("socials")

        findViewById<TextView>(R.id.profileTextView).text =
            "Welcome $username!\nEmail: $email\nSocials: $socials"
    }
}

