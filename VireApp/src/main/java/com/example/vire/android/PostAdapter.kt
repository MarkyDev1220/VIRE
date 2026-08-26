package com.example.vire.android

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doAfterTextChanged

class PostAdapter(
    private val context: Context,
    val posts: MutableList<Post>
) : BaseAdapter() {

    override fun getCount() = posts.size
    override fun getItem(position: Int) = posts[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_post, parent, false)

        val post = posts[position]

        val usernameText = view.findViewById<TextView>(R.id.usernameText)
        val contentText = view.findViewById<TextView>(R.id.contentText)
        val postImage = view.findViewById<ImageView>(R.id.postImage)
        val likeButton = view.findViewById<Button>(R.id.likeButton)
        val commentButton = view.findViewById<Button>(R.id.commentButton)

        // Username + content
        usernameText.text = post.username
        contentText.text = post.content

        // Image support
        if (post.imageUri != null) {
            postImage.visibility = View.VISIBLE
            try {
                postImage.setImageURI(Uri.parse(post.imageUri))
            } catch (e: Exception) {
                postImage.visibility = View.GONE
            }
        } else {
            postImage.visibility = View.GONE
        }

        // Like button
        likeButton.text = "Like (${post.likes})"
        likeButton.setOnClickListener {
            FeedManager.likePost(post.id)
            notifyDataSetChanged()
        }

        // Comment button
        commentButton.setOnClickListener {
            showCommentDialog(post)
        }

        return view
    }

    private fun showCommentDialog(post: Post) {
        val input = EditText(context)
        input.hint = "Write a comment..."

        AlertDialog.Builder(context)
            .setTitle("Add comment")
            .setView(input)
            .setPositiveButton("Post") { _, _ ->
                val commentText = input.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    val comment = Comment(
                        username = "User", // Replace with actual logged-in user later
                        text = commentText
                    )
                    post.comments.add(comment)
                    notifyDataSetChanged()
                    Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

