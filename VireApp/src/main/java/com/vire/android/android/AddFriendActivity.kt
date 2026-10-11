package com.vire.android.android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vire.android.R

class AddFriendActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        val closeBtn = findViewById<ImageButton>(R.id.closeAddFriend)
        val usernameInput = findViewById<EditText>(R.id.friendUsernameInput)
        val submitBtn = findViewById<Button>(R.id.addFriendSubmit)

        closeBtn.setOnClickListener {
            finish()
        }

        submitBtn.setOnClickListener {
            val query = usernameInput.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter a username or email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitBtn.isEnabled = false
            FriendManager.sendRequestByUsername(query) { success, message ->
                submitBtn.isEnabled = true
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                if (success) {
                    usernameInput.setText("")
                    finish()
                }
            }
        }
    }
}
