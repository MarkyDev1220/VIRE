package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.vire.android.R

enum class BottomTab {
    HOME,
    PROFILE,
    DECKS,
    COMMUNITIES,
    COMPETE,
    MESSAGES,
    MENU,
    NONE
}

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyStatusBarInsets()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyStatusBarInsets()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        applyStatusBarInsets()
    }

    private fun applyStatusBarInsets() {
        val rootChild = findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) ?: return
        val initialLeft = rootChild.paddingLeft
        val initialTop = rootChild.paddingTop
        val initialRight = rootChild.paddingRight
        val initialBottom = rootChild.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(rootChild) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            view.setPadding(
                initialLeft + maxOf(systemBars.left, displayCutout.left),
                initialTop + maxOf(systemBars.top, displayCutout.top),
                initialRight + maxOf(systemBars.right, displayCutout.right),
                initialBottom + maxOf(systemBars.bottom, displayCutout.bottom)
            )
            insets
        }
        ViewCompat.requestApplyInsets(rootChild)
    }

    override fun onResume() {
        super.onResume()
        if (FirebaseAuth.getInstance().currentUser != null) {
            NotificationHelper.ensureChannel(this)
            NotificationHelper.listenForNotifications(this)
        }
    }

    fun setupStandardBottomNav(activeTab: BottomTab = BottomTab.NONE) {
        if (findViewById<View>(R.id.bottomNav) == null) return

        val navHome = findViewById<View>(R.id.navHome)
        val navProfile = findViewById<View>(R.id.navProfile)
        val navDecks = findViewById<View>(R.id.navDecks)
        val navCommunities = findViewById<View>(R.id.navCommunities)
        val navMenu = findViewById<View>(R.id.navMenu)

        val selectedTab = when (activeTab) {
            BottomTab.COMPETE, BottomTab.MESSAGES, BottomTab.MENU -> BottomTab.MENU
            else -> activeTab
        }
        val navItems = listOf(
            Triple(findViewById<ImageView>(R.id.iconHome), findViewById<TextView>(R.id.textHome), BottomTab.HOME),
            Triple(findViewById<ImageView>(R.id.iconProfile), findViewById<TextView>(R.id.textProfile), BottomTab.PROFILE),
            Triple(findViewById<ImageView>(R.id.iconDecks), findViewById<TextView>(R.id.textDecks), BottomTab.DECKS),
            Triple(findViewById<ImageView>(R.id.iconCommunities), findViewById<TextView>(R.id.textCommunities), BottomTab.COMMUNITIES),
            Triple(findViewById<ImageView>(R.id.iconMenu), findViewById<TextView>(R.id.textMenu), BottomTab.MENU)
        )
        val activeColor = ContextCompat.getColor(this, R.color.circuit_lime)
        val inactiveColor = ContextCompat.getColor(this, R.color.circuit_text_secondary)
        navItems.forEach { (icon, label, tab) ->
            val color = if (selectedTab == tab) activeColor else inactiveColor
            icon?.setColorFilter(color)
            label?.setTextColor(color)
        }

        navHome?.setOnClickListener {
            if (this !is HomeActivity) {
                startActivity(Intent(this, HomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
            }
        }
        navProfile?.setOnClickListener {
            if (this !is ProfileActivity) startActivity(Intent(this, ProfileActivity::class.java))
        }
        navDecks?.setOnClickListener {
            if (this !is DeckListActivity) startActivity(Intent(this, DeckListActivity::class.java))
        }
        navCommunities?.setOnClickListener {
            if (this !is CommunityListActivity) startActivity(Intent(this, CommunityListActivity::class.java))
        }
        navMenu?.setOnClickListener { view -> showHamburgerMenu(view) }
    }

    fun showHamburgerMenu(anchor: View) {
        val items = listOf(
            Pair("🏠", "Home"),
            Pair("🔔", "Notifications"),
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
        val closeBtn = dialogView.findViewById<ImageButton>(R.id.closeMenuButton)

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
            .create()

        closeBtn?.setOnClickListener {
            dialog.dismiss()
        }

        gridView.setOnItemClickListener { _, _, position, _ ->
            val title = items[position].second
            dialog.dismiss()
            when (title) {
                "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                "Notifications" -> startActivity(Intent(this, NotificationsActivity::class.java))
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
