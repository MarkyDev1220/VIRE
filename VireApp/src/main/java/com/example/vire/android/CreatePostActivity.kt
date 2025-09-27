package com.example.vire.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class CreatePostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        val etPostContent = findViewById<EditText>(R.id.etPostContent)
        val btnSubmitPost = findViewById<Button>(R.id.btnSubmitPost)

        btnSubmitPost.setOnClickListener {
            val postText = etPostContent.text.toString().trim()

            if (postText.isNotEmpty()) {
                // Send post content back to HomeActivity
                val resultIntent = Intent()
                resultIntent.putExtra("new_post", postText)
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // Close CreatePostActivity
            } else {
                etPostContent.error = "Post cannot be empty"
            }
        }
    }
}
