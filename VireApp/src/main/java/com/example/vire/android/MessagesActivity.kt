package com.example.vire.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var addFriendBtn: Button
    private lateinit var chatInputBar: View
    private lateinit var leftPanel: View
    private lateinit var rightPanel: View

    private lateinit var usersAdapter: MessagesListAdapter
    private var chatAdapter: ChatAdapter? = null

    private var activeUser: String? = null
    private var isNewMessageMode = false

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

        recyclerUsers = findViewById(R.id.recyclerUsers)
        recyclerChat = findViewById(R.id.recyclerChatMessages)
        messageInput = findViewById(R.id.messageInput)
        sendBtn = findViewById(R.id.sendMessage)
        searchUsers = findViewById(R.id.searchUsers)
        chatHeader = findViewById(R.id.chatHeader)
        menuBtn = findViewById(R.id.hamburgerButton)
        newMessageBtn = findViewById(R.id.newMessageButton)
        addFriendBtn = findViewById(R.id.addFriendButton)
        chatInputBar = findViewById(R.id.chatInputBar)
        leftPanel = findViewById(R.id.leftPanel)
        rightPanel = findViewById(R.id.rightPanel)

        setupUsersList()
        setupSearchListener()
        setupSendMessage()
        setupHamburgerMenu()
        setupNewMessageButton()
        setupAddFriendButton()

        showConversationListMode()
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

        showChatMode()

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

            usersAdapter.updateData(MessageManager.getLastMessagesForAllUsers())

            messageInput.text.clear()
        }
    }

    private fun setupSearchListener() {
        searchUsers.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isNewMessageMode) {
                    usersAdapter.filter(s.toString())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun setupHamburgerMenu() {
        menuBtn.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menu.apply {
                add("Home")
                add("Profile")
                add("Messages")
                add("Buy/Sell")
                add("Challenges")
                add("Quest")
                add("Settings")
                add("Tournaments")
                add("Rankings")
                add("Friends")
                add("Search")
            }

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
                    "Friends" -> startActivity(Intent(this, FriendsActivity::class.java))
                    "Search" -> startActivity(Intent(this, SearchActivity::class.java))
                }
                true
            }

            popup.show()
        }
    }

    private fun setupNewMessageButton() {
        newMessageBtn.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menu.apply {
                add("Add Friend")
                add("Search for Users")
                add("New Message")
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Add Friend" -> {
                        startActivity(Intent(this, AddFriendActivity::class.java))
                    }
                    "Search for Users" -> {
                        isNewMessageMode = true
                        showNewMessageMode()
                    }
                    "New Message" -> {
                        activeUser = null
                        showConversationListMode()
                    }
                }
                true
            }

            popup.show()
        }
    }

    private fun setupAddFriendButton() {
        addFriendBtn.setOnClickListener {
            startActivity(Intent(this, AddFriendActivity::class.java))
        }
    }

    private fun showConversationListMode() {
        isNewMessageMode = false

        searchUsers.visibility = View.GONE
        chatInputBar.visibility = View.GONE
        chatHeader.visibility = View.GONE

        usersAdapter.updateData(MessageManager.getLastMessagesForAllUsers())
    }

    private fun showNewMessageMode() {
        isNewMessageMode = true

        searchUsers.visibility = View.VISIBLE
        chatInputBar.visibility = View.GONE
        chatHeader.visibility = View.GONE

        usersAdapter.updateData(MessageManager.getAllUsersAsMessageList())
    }

    private fun showChatMode() {
        searchUsers.visibility = View.GONE
        chatInputBar.visibility = View.VISIBLE
        chatHeader.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        usersAdapter.updateData(MessageManager.getLastMessagesForAllUsers())
        activeUser?.let { user ->
            chatAdapter?.notifyDataSetChanged()
            recyclerChat.scrollToPosition(MessageManager.getMessagesForUser(user).size - 1)
        }
    }
}


