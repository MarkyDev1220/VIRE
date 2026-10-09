package com.vire.android.android

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.vire.android.R

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
        val commentsContainer = view.findViewById<LinearLayout>(R.id.commentsContainer)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "demo_user"

        // Username + content
        usernameText.text = post.username
        contentText.text = post.content

        // Image support
        if (!post.imageUri.isNullOrEmpty()) {
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
        val isLiked = post.likedBy.contains(currentUid)
        val totalLikes = maxOf(post.likes, post.likedBy.size)
        likeButton.text = if (isLiked) "❤️ Liked ($totalLikes)" else "🤍 Like ($totalLikes)"
        likeButton.setOnClickListener {
            FeedManager.toggleLikePost(post.id, currentUid) {
                notifyDataSetChanged()
            }
        }

        // Comment button
        commentButton.text = "💬 Comment (${post.commentsCount})"
        commentButton.setOnClickListener {
            showCommentDialog(post)
        }

        // Comments display under post
        commentsContainer?.removeAllViews()
        FeedManager.getComments(post.id) { comments ->
            commentButton.text = "💬 Comment (${comments.size})"
            commentsContainer?.removeAllViews()
            if (comments.isNotEmpty()) {
                commentsContainer?.visibility = View.VISIBLE

                val header = TextView(context).apply {
                    text = "Comments (${comments.size}):"
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#FFFFFF"))
                    setPadding(4, 12, 4, 6)
                }
                commentsContainer?.addView(header)

                for (c in comments) {
                    val tv = TextView(context).apply {
                        val spannable = SpannableStringBuilder()
                        val authorName = if (c.username.isNotBlank()) c.username else "Gamer"
                        val boldUser = SpannableString(authorName)
                        boldUser.setSpan(
                            StyleSpan(Typeface.BOLD),
                            0,
                            boldUser.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        boldUser.setSpan(
                            ForegroundColorSpan(Color.parseColor("#64B5F6")),
                            0,
                            boldUser.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        spannable.append(boldUser)
                        spannable.append(": ")

                        val commentText = if (c.text.isNotBlank()) c.text else ""
                        spannable.append(commentText)

                        text = spannable
                        textSize = 14f
                        setTextColor(Color.WHITE)
                        setPadding(14, 10, 14, 10)
                        setBackgroundResource(R.drawable.comment_bubble_bg)
                    }

                    val showDeleteDialog = {
                        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        val myUsername = prefs.getString("username", "Gamer") ?: "Gamer"

                        val isCommentAuthor = (c.authorUid.isNotEmpty() && c.authorUid == currentUid) || c.username.equals(myUsername, ignoreCase = true)
                        val isPostAuthor = (post.authorUid.isNotEmpty() && post.authorUid == currentUid) || post.username.equals(myUsername, ignoreCase = true)

                        if (isCommentAuthor || isPostAuthor) {
                            AlertDialog.Builder(context)
                                .setTitle("Delete Comment")
                                .setMessage("Are you sure you want to delete this comment?")
                                .setPositiveButton("Delete") { _, _ ->
                                    FeedManager.deleteComment(post.id, c.id) {
                                        notifyDataSetChanged()
                                        Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        } else {
                            Toast.makeText(context, "You can only delete your own comments", Toast.LENGTH_SHORT).show()
                        }
                    }

                    tv.setOnClickListener { showDeleteDialog() }
                    tv.setOnLongClickListener {
                        showDeleteDialog()
                        true
                    }

                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 8
                    }
                    tv.layoutParams = lp
                    commentsContainer?.addView(tv)
                }
            } else {
                commentsContainer?.visibility = View.GONE
            }
        }

        return view
    }

    private fun showCommentDialog(post: Post) {
        val input = EditText(context).apply {
            hint = "Write a comment..."
            setPadding(32, 32, 32, 32)
        }

        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val realUsername = prefs.getString("username", "Gamer") ?: "Gamer"

        AlertDialog.Builder(context)
            .setTitle("Add comment")
            .setView(input)
            .setPositiveButton("Post") { _, _ ->
                val commentText = input.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    FeedManager.addComment(post.id, realUsername, commentText) {
                        notifyDataSetChanged()
                        Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
