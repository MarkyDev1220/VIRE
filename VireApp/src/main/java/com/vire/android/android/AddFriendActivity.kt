package com.vire.android.android

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.vire.android.R

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
