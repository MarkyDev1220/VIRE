package com.vire.android.android

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.vire.android.R

class AddFriendActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var submitBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var previewCard: CardView
    private lateinit var previewUsername: TextView
    private lateinit var previewDetails: TextView
    private lateinit var previewBio: TextView
    private lateinit var previewSendBtn: Button

    private var foundUser: User? = null
    private var friendshipState = FriendManager.FriendshipState.NONE
    private var pendingRequestId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        val closeBtn = findViewById<ImageButton>(R.id.closeAddFriend)
        usernameInput = findViewById(R.id.friendUsernameInput)
        submitBtn = findViewById(R.id.addFriendSubmit)
        progressBar = findViewById(R.id.addFriendProgress)
        previewCard = findViewById(R.id.previewCard)
        previewUsername = findViewById(R.id.previewUsername)
        previewDetails = findViewById(R.id.previewDetails)
        previewBio = findViewById(R.id.previewBio)
        previewSendBtn = findViewById(R.id.previewSendBtn)

        closeBtn.setOnClickListener { finish() }

        usernameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                performUserSearch()
                true
            } else {
                false
            }
        }

        submitBtn.setOnClickListener { performUserSearch() }

        previewSendBtn.setOnClickListener {
            val target = foundUser ?: return@setOnClickListener
            when (friendshipState) {
                FriendManager.FriendshipState.NONE -> {
                    previewSendBtn.isEnabled = false
                    FriendManager.sendFriendRequest(target.id) { success, message ->
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        if (success || message.contains("pending", ignoreCase = true) || message.contains("already sent", ignoreCase = true)) {
                            refreshPreviewFriendship(target)
                        } else {
                            previewSendBtn.isEnabled = true
                        }
                    }
                }
                FriendManager.FriendshipState.PENDING_SENT ->
                    Toast.makeText(this, "Your friend request is pending.", Toast.LENGTH_SHORT).show()
                FriendManager.FriendshipState.PENDING_RECEIVED -> showIncomingRequestActions(target)
                FriendManager.FriendshipState.FRIENDS ->
                    Toast.makeText(this, "You are already friends.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshPreviewFriendship(user: User) {
        FriendManager.getFriendshipState(user.id) { state, requestId ->
            if (foundUser?.id != user.id) return@getFriendshipState
            friendshipState = state
            pendingRequestId = requestId
            previewSendBtn.text = when (state) {
                FriendManager.FriendshipState.NONE -> "Send Friend Request"
                FriendManager.FriendshipState.PENDING_SENT -> "Request Pending"
                FriendManager.FriendshipState.PENDING_RECEIVED -> "Accept or Decline Request"
                FriendManager.FriendshipState.FRIENDS -> "Friends ✓"
            }
            previewSendBtn.isEnabled = state == FriendManager.FriendshipState.NONE ||
                state == FriendManager.FriendshipState.PENDING_RECEIVED
        }
    }

    private fun showIncomingRequestActions(user: User) {
        val requestId = pendingRequestId ?: return refreshPreviewFriendship(user)
        AlertDialog.Builder(this)
            .setTitle("Friend request")
            .setMessage("Accept the friend request from ${user.username}?")
            .setPositiveButton("Accept") { _, _ ->
                FriendManager.acceptRequest(requestId) { success ->
                    Toast.makeText(this, if (success) "You are now friends." else "Could not accept request.", Toast.LENGTH_SHORT).show()
                    refreshPreviewFriendship(user)
                }
            }
            .setNegativeButton("Decline") { _, _ ->
                FriendManager.declineRequest(requestId) { success ->
                    Toast.makeText(this, if (success) "Friend request declined." else "Could not decline request.", Toast.LENGTH_SHORT).show()
                    refreshPreviewFriendship(user)
                }
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun performUserSearch() {
        val query = usernameInput.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a username, name, or email", Toast.LENGTH_SHORT).show()
            return
        }

        submitBtn.isEnabled = false
        progressBar.visibility = View.VISIBLE
        previewCard.visibility = View.GONE
        foundUser = null

        FriendManager.findUserByQuery(query) { user ->
            submitBtn.isEnabled = true
            progressBar.visibility = View.GONE

            if (user == null) {
                Toast.makeText(this, "No user found matching '$query'", Toast.LENGTH_LONG).show()
            } else {
                foundUser = user
                previewUsername.text = user.username
                previewDetails.text = when {
                    user.localArea.isNotBlank() && user.skillLevel.isNotBlank() -> "📍 ${user.localArea} • ${user.skillLevel}"
                    user.localArea.isNotBlank() -> "📍 ${user.localArea}"
                    user.skillLevel.isNotBlank() -> user.skillLevel
                    else -> "VIRE Gamer"
                }
                previewBio.text = when {
                    user.gamerBio.isNotBlank() -> user.gamerBio
                    user.favoriteGames.isNotEmpty() -> "Plays: ${user.favoriteGames.joinToString(", ")}"
                    else -> "No bio provided"
                }
                previewCard.visibility = View.VISIBLE

                // Check current friendship state
                FriendManager.getFriendshipState(user.id) { state, requestId ->
                    friendshipState = state
                    pendingRequestId = requestId
                    when (state) {
                        FriendManager.FriendshipState.FRIENDS -> {
                            previewSendBtn.text = "Friends ✓"
                            previewSendBtn.isEnabled = false
                            previewSendBtn.setBackgroundColor(Color.parseColor("#616161"))
                        }
                        FriendManager.FriendshipState.PENDING_SENT -> {
                            previewSendBtn.text = "Request Pending"
                            previewSendBtn.isEnabled = false
                            previewSendBtn.setBackgroundColor(Color.parseColor("#9E9E9E"))
                        }
                        FriendManager.FriendshipState.PENDING_RECEIVED -> {
                            previewSendBtn.text = "Accept Request"
                            previewSendBtn.isEnabled = true
                            previewSendBtn.setBackgroundColor(Color.parseColor("#388E3C"))
                        }
                        FriendManager.FriendshipState.NONE -> {
                            previewSendBtn.text = "Send Friend Request"
                            previewSendBtn.isEnabled = true
                            previewSendBtn.setBackgroundColor(Color.parseColor("#388E3C"))
                        }
                    }
                }
            }
        }
    }
}
