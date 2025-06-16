package com.example.vire.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val welcomeText: TextView = findViewById(R.id.welcomeText)
        welcomeText.text = "Welcome to the Home Screen!"
    }
}


