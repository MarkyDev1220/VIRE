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

    fun showHamburgerMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        val menuItems = listOf(
            "Home",
            "Profile",
            "Deck Builder",
            "My Collection",
            "Gaming Locations",
            "Gaming Communities",
            "Game Nights",
            "Tournaments",
            "Find Players",
            "Search",
            "Friends",
            "Buy/Sell",
            "Messages",
            "Challenges",
            "Quest",
            "Settings"
        )
        menuItems.forEach { popup.menu.add(it) }

        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                "Profile" -> startActivity(Intent(this, ProfileActivity::class.java))
                "Deck Builder" -> startActivity(Intent(this, DeckListActivity::class.java))
                "My Collection" -> startActivity(Intent(this, CollectionActivity::class.java))
                "Gaming Locations" -> startActivity(Intent(this, LocationListActivity::class.java))
                "Gaming Communities" -> startActivity(Intent(this, CommunityListActivity::class.java))
                "Game Nights" -> startActivity(Intent(this, GameNightListActivity::class.java))
                "Tournaments" -> startActivity(Intent(this, TournamentsActivity::class.java))
                "Find Players" -> startActivity(Intent(this, PlayerFinderActivity::class.java))
                "Search" -> startActivity(Intent(this, SearchActivity::class.java))
                "Friends" -> startActivity(Intent(this, FriendsActivity::class.java))
                "Buy/Sell" -> startActivity(Intent(this, BuySellActivity::class.java))
                "Messages" -> startActivity(Intent(this, MessagesActivity::class.java))
                "Challenges" -> startActivity(Intent(this, ChallengesActivity::class.java))
                "Quest" -> startActivity(Intent(this, QuestActivity::class.java))
                "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            true
        }
        popup.show()
    }
}
