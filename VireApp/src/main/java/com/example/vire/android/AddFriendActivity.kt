package com.example.vire.android

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class AddFriendActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        val closeBtn = findViewById<ImageButton>(R.id.closeAddFriend)
        closeBtn.setOnClickListener {
            finish() // closes the Add Friend screen
        }
    }
}
