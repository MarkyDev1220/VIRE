package com.vire.android.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vire.android.R

class NewMessagesActivity : AppCompatActivity() {

    private lateinit var searchUserEditText: EditText
    private lateinit var userListView: ListView
    private val allUsers = mutableListOf<String>()
    private val filteredUsers = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_messages)

        searchUserEditText = findViewById(R.id.searchUserEditText)
        userListView = findViewById(R.id.userListView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredUsers)
        userListView.adapter = adapter

        searchUserEditText.addTextChangedListener { text ->
            val query = text?.toString().orEmpty().trim()
            filteredUsers.clear()
            filteredUsers.addAll(allUsers.filter { it.contains(query, ignoreCase = true) })
            adapter.notifyDataSetChanged()
        }

        userListView.setOnItemClickListener { _, _, position, _ ->
            setResult(Activity.RESULT_OK, Intent().putExtra("selected_user", filteredUsers[position]))
            finish()
        }

        FirebaseFirestore.getInstance().collection("users").get()
            .addOnSuccessListener { snapshot ->
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                allUsers.clear()
                allUsers.addAll(snapshot.documents.mapNotNull { doc ->
                    if (doc.id == currentUid) return@mapNotNull null
                    doc.getString("username") ?: doc.getString("fullName") ?: doc.getString("name")
                }.filter { it.isNotBlank() }.distinct().sorted())
                filteredUsers.clear()
                filteredUsers.addAll(allUsers)
                adapter.notifyDataSetChanged()
            }
    }
}
