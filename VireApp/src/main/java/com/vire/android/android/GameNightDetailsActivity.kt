package com.vire.android.android

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.vire.android.R

class GameNightDetailsActivity : AppCompatActivity() {

    private lateinit var gameNight: GameNight
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_night_details)

        gameNight = if (Build.VERSION.SDK_INT >= 33) {
            intent.getSerializableExtra("game_night", GameNight::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("game_night") as GameNight
        }

        val title: TextView = findViewById(R.id.detailTitle)
        val host: TextView = findViewById(R.id.detailHost)
        val dateTime: TextView = findViewById(R.id.detailDateTime)
        val location: TextView = findViewById(R.id.detailLocation)
        val participants: TextView = findViewById(R.id.detailParticipants)
        val desc: TextView = findViewById(R.id.detailDesc)
        val btnJoinLeave: Button = findViewById(R.id.btnJoinLeave)
        val btnDelete: Button = findViewById(R.id.btnDelete)

        title.text = gameNight.gameTitle
        host.text = "Hosted by ${gameNight.hostUsername}"
        dateTime.text = "${gameNight.date} at ${gameNight.time}"
        location.text = "Location: ${gameNight.location}"
        participants.text = "Participants: ${gameNight.participants.size} / ${gameNight.maxParticipants}"
        desc.text = gameNight.description

        val currentUid = auth.currentUser?.uid ?: ""
        val isHost = gameNight.hostUid == currentUid
        val isJoined = gameNight.participants.contains(currentUid)

        if (isHost) {
            btnDelete.visibility = View.VISIBLE
            btnJoinLeave.visibility = View.GONE
        } else {
            btnJoinLeave.text = if (isJoined) "Leave Game Night" else "Join Game Night"
        }

        btnJoinLeave.setOnClickListener {
            if (isJoined) {
                GameNightManager.leaveGameNight(gameNight.id) { success ->
                    if (success) finish()
                }
            } else {
                if (gameNight.participants.size >= gameNight.maxParticipants) {
                    Toast.makeText(this, "Game Night is full!", Toast.LENGTH_SHORT).show()
                } else {
                    GameNightManager.joinGameNight(gameNight.id) { success ->
                        if (success) finish()
                    }
                }
            }
        }

        btnDelete.setOnClickListener {
            GameNightManager.deleteGameNight(gameNight.id) { success ->
                if (success) finish()
            }
        }
    }
}
