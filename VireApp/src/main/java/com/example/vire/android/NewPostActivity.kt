package com.example.vire.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NewPostActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_post)

        val postEditText: EditText = findViewById(R.id.postEditText)
        val addPhoto: ImageButton = findViewById(R.id.addPhotoButton)
        val addVideo: ImageButton = findViewById(R.id.addVideoButton)
        val postButton: Button = findViewById(R.id.postButton)

        val username = intent.getStringExtra("username") ?: "User"

        postButton.setOnClickListener {
            val content = postEditText.text.toString().trim()

            if (content.isNotEmpty()) {
                val post = Post(username, content)

                FeedManager.addPost(post)

                val resultIntent = Intent()
                resultIntent.putExtra("new_post", post)
                setResult(Activity.RESULT_OK, resultIntent)

                Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Write something before posting!", Toast.LENGTH_SHORT).show()
            }
        }

        addPhoto.setOnClickListener {
            Toast.makeText(this, "Add Photo feature coming soon", Toast.LENGTH_SHORT).show()
        }

        addVideo.setOnClickListener {
            Toast.makeText(this, "Add Video feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }


}

