package com.vire.android.android

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.vire.android.R

class BlockedUsersActivity : BaseActivity() {
    private lateinit var listContainer: LinearLayout
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_users)
        setupStandardBottomNav(BottomTab.MENU)
        listContainer = findViewById(R.id.blockedUsersList)
        emptyText = findViewById(R.id.emptyBlockedUsers)
    }

    override fun onResume() {
        super.onResume()
        loadBlockedUsers()
    }

    private fun loadBlockedUsers() {
        listContainer.removeAllViews()
        TrustSafetyManager.getBlockedUsers { users ->
            listContainer.removeAllViews()
            emptyText.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            users.forEach { user ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(16, 12, 8, 12)
                    setBackgroundResource(R.drawable.circuit_card_bg)
                }
                val name = TextView(this).apply {
                    text = user.username
                    textSize = 16f
                    setTextColor(getColor(R.color.circuit_text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val unblock = Button(this).apply {
                    text = "Unblock"
                    setOnClickListener {
                        TrustSafetyManager.unblockUser(user.uid) { success ->
                            if (success) {
                                Toast.makeText(this@BlockedUsersActivity, "Player unblocked", Toast.LENGTH_SHORT).show()
                                loadBlockedUsers()
                            } else {
                                Toast.makeText(this@BlockedUsersActivity, "Could not unblock player", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                row.addView(name)
                row.addView(unblock)
                listContainer.addView(row, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 })
            }
        }
    }
}
