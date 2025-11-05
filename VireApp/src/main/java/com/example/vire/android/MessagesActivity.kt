package com.example.vire.android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView


class MessagesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        // Hamburger menu
        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener {
            val popup = android.widget.PopupMenu(this, it)
            popup.menu.add("Home")
            popup.menu.add("Profile")
            popup.menu.add("Messages")
            popup.menu.add("Buy/Sell")
            popup.menu.add("Challenges")
            popup.menu.add("Quest")
            popup.menu.add("Settings")
            popup.menu.add("Tournaments")
            popup.menu.add("Rankings")

            popup.setOnMenuItemClickListener { item ->
                when(item.title.toString()) {
                    "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                    "Profile" -> startActivity(Intent(this, ProfileActivity::class.java))
                    "Messages" -> startActivity(Intent(this, MessagesActivity::class.java))
                    "Buy/Sell" -> startActivity(Intent(this, BuySellActivity::class.java))
                    "Challenges" -> startActivity(Intent(this, ChallengesActivity::class.java))
                    "Quest" -> startActivity(Intent(this, QuestActivity::class.java))
                    "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                    "Tournaments" -> startActivity(Intent(this, TournamentsActivity::class.java))
                    "Rankings" -> startActivity(Intent(this, RankingsActivity::class.java))
                }
                true
            }
            popup.show()
        }

        findViewById<TextView>(R.id.messagesText).text = "Messages Page"
    }
}

