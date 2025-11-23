package com.example.vire.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MessagesActivity : BaseActivity() {

    private lateinit var recyclerUsers: RecyclerView
    private lateinit var recyclerChat: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var searchUsers: EditText
    private lateinit var chatHeader: TextView
    private lateinit var menuBtn: ImageButton
    private lateinit var newMessageBtn: ImageButton

    private lateinit var usersAdapter: MessagesListAdapter
    private var chatAdapter: ChatAdapter? = null

    private var activeUser: String? = null

    private val newMessageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedUser = result.data?.getStringExtra("selected_user") ?: return@registerForActivityResult
                openChatWithUser(selectedUser)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        // Initialize views
        recyclerUsers = findViewById(R.id.recyclerUsers)
        recyclerChat = findViewById(R.id.recyclerChatMessages)
        messageInput = findViewById(R.id.messageInput)
        sendBtn = findViewById(R.id.sendMessage)
        searchUsers = findViewById(R.id.searchUsers)
        chatHeader = findViewById(R.id.chatHeader)
        menuBtn = findViewById(R.id.hamburgerButton)
        newMessageBtn = findViewById(R.id.newMessageButton)

        setupUsersList()
        setupSearchListener()
        setupSendMessage()
        setupMenu()
        setupNewMessageButton()
    }

    private fun setupUsersList() {
        usersAdapter = MessagesListAdapter(
            MessageManager.getLastMessagesForAllUsers()
        ) { selectedUser ->
            openChatWithUser(selectedUser)
        }

        recyclerUsers.adapter = usersAdapter
        recyclerUsers.layoutManager = LinearLayoutManager(this)
    }

    private fun openChatWithUser(selectedUser: String) {
        activeUser = selectedUser
        chatHeader.text = selectedUser

        val messages = MessageManager.getMessagesForUser(selectedUser)
        chatAdapter = ChatAdapter(messages)
        recyclerChat.adapter = chatAdapter
        recyclerChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        recyclerChat.scrollToPosition(messages.size - 1)
    }

    private fun setupSendMessage() {
        sendBtn.setOnClickListener {
            val user = activeUser ?: return@setOnClickListener
            val text = messageInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val newMessage = Message("Me", text)
            MessageManager.sendMessageForUser(user, newMessage)

            chatAdapter?.notifyItemInserted(MessageManager.getMessagesForUser(user).size - 1)
            recyclerChat.scrollToPosition(MessageManager.getMessagesForUser(user).size - 1)

            // Update user list preview immediately
            usersAdapter.updateData(MessageManager.getLastMessagesForAllUsers())

            messageInput.text.clear()
        }
    }

    private fun setupSearchListener() {
        searchUsers.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                usersAdapter.filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupMenu() {
        menuBtn.setOnClickListener { button ->
            val popup = PopupMenu(this, button)
            val menuItems = listOf(
                "Home", "Profile", "Messages", "Buy/Sell", "Challenges",
                "Quest", "Settings", "Tournaments", "Rankings", "Friends", "Search"
            )
            menuItems.forEach { popup.menu.add(it) }

            popup.setOnMenuItemClickListener { item ->
                when(item.title.toString()) {
                    "Home" -> startActivity(Intent(this, HomeActivity::class.java))
                    "Profile" -> startActivity(Intent(this, ProfileActivity::class.java))
                    "Messages" -> startActivity(Intent(this, MessagesActivity::class.java))
                    "Buy/Sell" -> startActivity(Intent(this, BuySellActivity::class.java))
                    "Challenges" -> startActivity(Intent(this, ChallengesActivity::class.java))
                    "Quest" -> startActivity(Intent(this, QuestActivity::class.java))
                    "Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                    "Tournaments" -> startActivity(Intent(this, TournamentsActivity::class.java))
                    "Rankings" -> startActivity(Intent(this, RankingsActivity::class.java))
                    "Friends" -> startActivity(Intent(this, FriendsActivity::class.java))
                    "Search" -> startActivity(Intent(this, SearchActivity::class.java))
                }
                true
            }
            popup.show()
        }
    }

    private fun setupNewMessageButton() {
        newMessageBtn.setOnClickListener {
            val intent = Intent(this, NewMessagesActivity::class.java)
            newMessageLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh user list and active chat if any
        usersAdapter.updateData(MessageManager.getLastMessagesForAllUsers())
        activeUser?.let { user ->
            chatAdapter?.notifyDataSetChanged()
            recyclerChat.scrollToPosition(MessageManager.getMessagesForUser(user).size - 1)
        }
    }
}

