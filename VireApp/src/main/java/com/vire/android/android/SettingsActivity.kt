package com.vire.android.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupHamburgerMenu()

        val sharedPref = getSharedPreferences("VirePrefs", MODE_PRIVATE)

        findViewById<LinearLayout>(R.id.changeProfileOption).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.changePasswordOption).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.logoutOption).setOnClickListener {
            performLogout()
        }

        findViewById<LinearLayout>(R.id.deleteProfileOption).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Yes, Delete") { _, _ ->
                    performAccountDeletion()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

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

        val darkModeSwitch = findViewById<Switch>(R.id.darkModeSwitch)
        darkModeSwitch.isChecked = sharedPref.getBoolean("dark_mode", false)
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
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

        findViewById<LinearLayout>(R.id.contactSupportOption).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@vire.com")
                putExtra(Intent.EXTRA_SUBJECT, "Support Request")
            }
            startActivity(Intent.createChooser(intent, "Contact Support"))
        }

        findViewById<LinearLayout>(R.id.termsOption).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://vire.com/terms")))
        }
    }

    private fun performLogout() {
        clearLocalSession(this)

        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Ignore signout error
        }

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun performAccountDeletion() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val uid = user?.uid ?: getSharedPreferences("user_prefs", MODE_PRIVATE).getString("uid", null)

        if (!uid.isNullOrEmpty()) {
            // Delete Firestore user document
            try {
                FirebaseFirestore.getInstance().collection("users").document(uid).delete()
            } catch (e: Exception) {
                // Ignore Firestore delete error
            }
        }

        // Delete Firebase Auth User
        if (user != null) {
            user.delete().addOnCompleteListener {
                finishDeletionAndRedirect()
            }
        } else {
            finishDeletionAndRedirect()
        }
    }

    private fun finishDeletionAndRedirect() {
        clearLocalSession(this)

        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Ignore signout error
        }

        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun clearLocalSession(context: Context) {
        val prefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()

        val virePrefs = context.getSharedPreferences("VirePrefs", MODE_PRIVATE)
        virePrefs.edit().clear().apply()
    }
}
