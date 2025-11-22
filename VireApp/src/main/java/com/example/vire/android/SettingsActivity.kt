package com.example.vire.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val sharedPref = getSharedPreferences("VirePrefs", MODE_PRIVATE)

        // Hamburger menu
        val hamburgerButton = findViewById<ImageButton>(R.id.hamburgerButton)
        hamburgerButton.setOnClickListener { showMenu(it) }

        // ================== Account ==================
        findViewById<LinearLayout>(R.id.changeProfileOption).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.changePasswordOption).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.logoutOption).setOnClickListener {
            // Clear user preferences and return to login
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))

            finishAffinity()
        }

        // ================== Notifications ==================
        val pushSwitch = findViewById<Switch>(R.id.pushNotificationsSwitch)
        pushSwitch.isChecked = sharedPref.getBoolean("push_notifications", true)
        pushSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("push_notifications", isChecked).apply()
        }

        val emailSwitch = findViewById<Switch>(R.id.emailNotificationsSwitch)
        emailSwitch.isChecked = sharedPref.getBoolean("email_notifications", true)
        emailSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("email_notifications", isChecked).apply()
        }

        // ================== App Preferences ==================
        val darkModeSwitch = findViewById<Switch>(R.id.darkModeSwitch)
        darkModeSwitch.isChecked = sharedPref.getBoolean("dark_mode", false)
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            recreate()
        }

        findViewById<LinearLayout>(R.id.languageOption).setOnClickListener {
            val languages = arrayOf("English", "Spanish", "French")
            AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(languages) { _, which ->
                    sharedPref.edit().putString("app_language", languages[which]).apply()
                }
                .show()
        }

        findViewById<LinearLayout>(R.id.defaultPageOption).setOnClickListener {
            val pages = arrayOf("Home", "Profile", "Messages")
            AlertDialog.Builder(this)
                .setTitle("Default Landing Page")
                .setItems(pages) { _, which ->
                    sharedPref.edit().putString("default_page", pages[which]).apply()
                }
                .show()
        }

        // ================== Privacy & Security ==================
        findViewById<LinearLayout>(R.id.blockedUsersOption).setOnClickListener {
            startActivity(Intent(this, BlockedUsersActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.twoFactorOption).setOnClickListener {
            startActivity(Intent(this, TwoFactorActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.dataSharingOption).setOnClickListener {
            val options = arrayOf("Allow all", "Allow essential only", "No data sharing")
            AlertDialog.Builder(this)
                .setTitle("Data Sharing Preferences")
                .setItems(options) { _, which ->
                    sharedPref.edit().putInt("data_sharing", which).apply()
                }
                .show()
        }

        // ================== Support & About ==================
        findViewById<LinearLayout>(R.id.contactSupportOption).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@vire.com")
                putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            }
            startActivity(Intent.createChooser(intent, "Contact Support"))
        }

        findViewById<LinearLayout>(R.id.termsOption).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vire.com/terms"))
            startActivity(browserIntent)
        }

        // App version is just a TextView, no action needed
    }

    private fun showMenu(view: View) {
        val popup = android.widget.PopupMenu(this, view)
        val menuItems = listOf(
            "Home", "Profile", "Messages", "Buy/Sell", "Challenges",
            "Quest", "Settings", "Tournaments", "Rankings"
        )
        menuItems.forEach { popup.menu.add(it) }

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
            }
            true
        }
        popup.show()
    }
}
