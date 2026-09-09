package com.vire.android.android

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.vire.android.R

class NewPostActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null

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

        val postEditText: EditText = findViewById(R.id.postEditText)
        val addPhoto: ImageButton = findViewById(R.id.addPhotoButton)
        val addVideo: ImageButton = findViewById(R.id.addVideoButton)
        val postButton: Button = findViewById(R.id.postButton)
        val closeButton: ImageButton = findViewById(R.id.closeNewPost)
        val userNameText: TextView = findViewById(R.id.userName)

        // ✅ Always load username from SharedPreferences
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = prefs.getString("username", "Unknown") ?: "Unknown"

        // ✅ Update the UI with the actual username
        userNameText.text = username

        closeButton.setOnClickListener { finish() }

        addPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        addVideo.setOnClickListener {
            Toast.makeText(this, "Video posting coming soon!", Toast.LENGTH_SHORT).show()
        }

        postButton.setOnClickListener {
            val content = postEditText.text.toString().trim()

            if (content.isEmpty() && selectedImageUri == null) {
                Toast.makeText(this, "Write something or add an image!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Add post with correct username
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
