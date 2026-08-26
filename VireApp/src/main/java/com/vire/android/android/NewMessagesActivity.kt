package com.vire.android.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.vire.android.R

class NewMessagesActivity : AppCompatActivity() {

    private lateinit var searchUserEditText: EditText
    private lateinit var userListView: ListView

    private val allUsers = listOf(
        "Alice",
        "Bob",
        "Charlie",
        "David",
        "Maria",
        "Jessie"
    )

    private val filteredUsers = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_new_messages)

        searchUserEditText = findViewById(R.id.searchUserEditText)
        userListView = findViewById(R.id.userListView)

        filteredUsers.clear()
        filteredUsers.addAll(allUsers)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredUsers)
        userListView.adapter = adapter

        // Live search filter
        searchUserEditText.addTextChangedListener { text ->
            val query = text.toString().lowercase()

            filteredUsers.clear()
            filteredUsers.addAll(
                allUsers.filter { it.lowercase().contains(query) }
            )
            adapter.notifyDataSetChanged()
        }

        // When user selects a name, send back to MessagesActivity
        userListView.setOnItemClickListener { _, _, position, _ ->
            val username = filteredUsers[position]

            val returnIntent = Intent().apply {
                putExtra("selected_user", username)
            }

            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }
}
