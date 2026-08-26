package com.example.vire.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class NewPostActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null

    // Image picker launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            Toast.makeText(this, "Image selected!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_post)

        // UI elements
        val postEditText: EditText = findViewById(R.id.postEditText)
        val addPhoto: ImageButton = findViewById(R.id.addPhotoButton)
        val addVideo: ImageButton = findViewById(R.id.addVideoButton)
        val postButton: Button = findViewById(R.id.postButton)
        val closeButton: ImageButton = findViewById(R.id.closeNewPost)

        val username = intent.getStringExtra("username") ?: "User"

        // Close (X) button
        closeButton.setOnClickListener {
            finish()
        }

        // Photo picker
        addPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Video picker placeholder
        addVideo.setOnClickListener {
            Toast.makeText(this, "Video posting coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Post button logic
        postButton.setOnClickListener {
            val content = postEditText.text.toString().trim()

            if (content.isEmpty() && selectedImageUri == null) {
                Toast.makeText(this, "Write something or add an image!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add post to feed
            FeedManager.addPost(
                username = username,
                content = content,
                imageUri = selectedImageUri?.toString()
            )

            Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show()

            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}



