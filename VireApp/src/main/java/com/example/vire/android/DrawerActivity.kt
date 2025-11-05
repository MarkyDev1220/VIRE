package com.example.vire.android

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class DrawerActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.navigation_view)
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> showToast("Home clicked")
                R.id.nav_profile -> showToast("Profile clicked")
                R.id.nav_messages -> showToast("Messages clicked")
                R.id.nav_buy_sell -> showToast("Buy/Sell clicked")
                R.id.nav_challenges -> showToast("Challenges clicked")
                R.id.nav_quests -> showToast("Quest clicked")
                R.id.nav_settings -> showToast("Settings clicked")
                R.id.nav_tournaments -> showToast("Tournaments clicked")
                R.id.nav_Rankings -> showToast("Rankings clicked")
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }
}

