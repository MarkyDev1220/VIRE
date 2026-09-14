package com.vire.android.android

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.vire.android.R

class GameNightListActivity : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val gameNights = mutableListOf<GameNight>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_night_list)

        setupHamburgerMenu()

        listView = findViewById(R.id.gameNightListView)
        val btnHost: Button = findViewById(R.id.btnHostNew)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        btnHost.setOnClickListener {
            startActivity(Intent(this, CreateGameNightActivity::class.java))
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, GameNightDetailsActivity::class.java)
            intent.putExtra("game_night", gameNights[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadGameNights()
    }

    private fun loadGameNights() {
        GameNightManager.getGameNights { list ->
            gameNights.clear()
            gameNights.addAll(list)

            val displayList = list.map { 
                "${it.gameTitle}\n${it.date} @ ${it.time}\nHosted by ${it.hostUsername}"
            }
            adapter.clear()
            adapter.addAll(displayList)
            adapter.notifyDataSetChanged()
        }
    }
}
