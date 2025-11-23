package com.example.vire.android

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doAfterTextChanged

class PostAdapter(
    private val context: Context,
    private val posts: MutableList<Post>
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
        val likeButton = view.findViewById<Button>(R.id.likeButton)
        val commentButton = view.findViewById<Button>(R.id.commentButton)

        usernameText.text = post.username
        contentText.text = post.content
        likeButton.text = "Like (${post.likes})"

        likeButton.setOnClickListener {
            post.likes++
            notifyDataSetChanged()
        }

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
                val comment = input.text.toString().trim()
                if (comment.isNotEmpty()) {
                    post.comments.add(comment)
                    notifyDataSetChanged()
                    Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
