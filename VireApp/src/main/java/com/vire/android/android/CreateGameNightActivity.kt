package com.vire.android.android

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.vire.android.R
import java.util.*

class CreateGameNightActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_game_night)

        val titleInput: EditText = findViewById(R.id.editGameTitle)
        val dateInput: EditText = findViewById(R.id.editDate)
        val timeInput: EditText = findViewById(R.id.editTime)
        val locationInput: EditText = findViewById(R.id.editLocation)
        val maxInput: EditText = findViewById(R.id.editMaxParticipants)
        val descInput: EditText = findViewById(R.id.editDescription)
        val btnCreate: Button = findViewById(R.id.btnCreate)
        val btnCancel: Button = findViewById(R.id.btnCancel)

        val calendar = Calendar.getInstance()

        dateInput.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                dateInput.setText("${m + 1}/$d/$y")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        timeInput.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                timeInput.setText(String.format("%02d:%02d", h, min))
            }, 19, 0, true).show()
        }

        btnCancel.setOnClickListener { finish() }

        btnCreate.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val date = dateInput.text.toString().trim()
            val time = timeInput.text.toString().trim()
            val location = locationInput.text.toString().trim()
            val max = maxInput.text.toString().toIntOrNull() ?: 4
            val desc = descInput.text.toString().trim()

            if (title.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Please fill in title, date, and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val username = prefs.getString("username", "Unknown") ?: "Unknown"

            val gameNight = GameNight(
                hostUsername = username,
                gameTitle = title,
                date = date,
                time = time,
                location = location,
                description = desc,
                maxParticipants = max
            )

            GameNightManager.createGameNight(gameNight) { success ->
                if (success) {
                    Toast.makeText(this, "Game Night Hosted!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to create Game Night", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
