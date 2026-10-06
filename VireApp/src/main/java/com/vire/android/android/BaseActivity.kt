package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
        val items = listOf(
            Pair("🏠", "Home"),
            Pair("👤", "Profile"),
            Pair("🃏", "Deck Builder"),
            Pair("🎒", "Collection"),
            Pair("📍", "Locations"),
            Pair("💬", "Communities"),
            Pair("🎲", "Game Nights"),
            Pair("🏆", "Tournaments"),
            Pair("👥", "Find Players"),
            Pair("🔍", "Search"),
            Pair("🤝", "Friends"),
            Pair("🏷️", "Buy / Sell"),
            Pair("📩", "Messages"),
            Pair("⚔️", "Challenges"),
            Pair("⚙️", "Settings")
        )

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_hamburger_menu, null)
        val gridView = dialogView.findViewById<GridView>(R.id.menuGridView)

        val gridAdapter = object : ArrayAdapter<Pair<String, String>>(this, R.layout.item_menu_grid, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_menu_grid, parent, false)
                val item = getItem(position)
                v.findViewById<TextView>(R.id.menuIcon).text = item?.first ?: ""
                v.findViewById<TextView>(R.id.menuTitle).text = item?.second ?: ""
                return v
            }
        }

        gridView.adapter = gridAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()

        gridView.setOnItemClickListener { _, _, position, _ ->
            val title = items[position].second
            dialog.dismiss()
            when (title) {
                "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                "Profile" -> startActivity(Intent(this, ProfileActivity::class.java))
                "Deck Builder" -> startActivity(Intent(this, DeckListActivity::class.java))
                "Collection" -> startActivity(Intent(this, CollectionActivity::class.java))
                "Locations" -> startActivity(Intent(this, LocationListActivity::class.java))
                "Communities" -> startActivity(Intent(this, CommunityListActivity::class.java))
                "Game Nights" -> startActivity(Intent(this, GameNightListActivity::class.java))
                "Tournaments" -> startActivity(Intent(this, TournamentsActivity::class.java))
                "Find Players" -> startActivity(Intent(this, PlayerFinderActivity::class.java))
                "Search" -> startActivity(Intent(this, SearchActivity::class.java))
                "Friends" -> startActivity(Intent(this, FriendsActivity::class.java))
                "Buy / Sell" -> startActivity(Intent(this, BuySellActivity::class.java))
                "Messages" -> startActivity(Intent(this, MessagesActivity::class.java))
                "Challenges" -> startActivity(Intent(this, ChallengesActivity::class.java))
                "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
            }
        }

        dialog.show()
    }
}
