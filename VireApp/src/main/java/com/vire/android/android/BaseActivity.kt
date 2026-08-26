package com.vire.android.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.vire.android.R

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected open fun setupHamburgerMenu() {
        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton?.setOnClickListener { view ->
            showHamburgerMenu(view)
        }
    }

    private fun showHamburgerMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Home")
        popup.menu.add("Profile")
        popup.menu.add("Messages")
        popup.menu.add("Buy/Sell")
        popup.menu.add("Challenges")
        popup.menu.add("Quest")
        popup.menu.add("Settings")
        popup.menu.add("Tournaments")
        popup.menu.add("Rankings")
        popup.menu.add("Friends")
        popup.menu.add("Search")

        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                "Profile" -> startActivity(Intent(this, ProfileActivity::class.java))
                "Messages" -> startActivity(Intent(this, MessagesActivity::class.java))
                "Buy/Sell" -> startActivity(Intent(this, BuySellActivity::class.java))
                "Challenges" -> startActivity(Intent(this, ChallengesActivity::class.java))
                "Quest" -> startActivity(Intent(this, QuestActivity::class.java))
                "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                "Tournaments" -> startActivity(Intent(this, TournamentsActivity::class.java))
                "Rankings" -> startActivity(Intent(this, RankingsActivity::class.java))
                "Friends" -> startActivity(Intent(this, FriendsActivity::class.java))
                "Search" -> startActivity(Intent(this, SearchActivity::class.java))
            }
            true
        }
        popup.show()
    }
}
