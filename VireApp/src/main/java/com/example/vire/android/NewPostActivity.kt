package com.example.vire.android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vire.R

class NewPostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.vire.android.R.layout.activity_new_post)

        val postEditText: EditText = findViewById(com.example.vire.android.R.id.postEditText)
        val addPhoto: ImageButton = findViewById(com.example.vire.android.R.id.addPhotoButton)
        val addVideo: ImageButton = findViewById(com.example.vire.android.R.id.addVideoButton)
        val postButton: Button = findViewById(com.example.vire.android.R.id.postButton)

        postButton.setOnClickListener {
            val postContent = postEditText.text.toString().trim()
            if (postContent.isNotEmpty()) {
                Toast.makeText(this, "Post created: $postContent", Toast.LENGTH_SHORT).show()
                // TODO: Save post to backend or feed
                finish()
            } else {
                Toast.makeText(this, "Write something before posting!", Toast.LENGTH_SHORT).show()
            }
        }

        addPhoto.setOnClickListener {
            Toast.makeText(this, "Add Photo option clicked", Toast.LENGTH_SHORT).show()
        }

        addVideo.setOnClickListener {
            Toast.makeText(this, "Add Video option clicked", Toast.LENGTH_SHORT).show()
        }
    }
}
