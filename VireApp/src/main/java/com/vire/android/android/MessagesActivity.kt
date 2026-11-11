package com.vire.android.android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vire.android.R

class MessagesActivity : BaseActivity() {

    // Containers
    private lateinit var inboxContainer: View
    private lateinit var chatContainer: View

    // Inbox Views
    private lateinit var btnInboxNotifications: ImageButton
    private lateinit var btnInboxProfile: ImageView
    private lateinit var searchMessagesInput: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var btnNewChat: Button
    private lateinit var tabAll: TextView
    private lateinit var tabUnread: TextView
    private lateinit var tabGroups: TextView
    private lateinit var cardFriendsRequests: CardView
    private lateinit var btnMarkAllRead: TextView
    private lateinit var tvSearchUsersHeader: TextView
    private lateinit var recyclerSearchUsers: RecyclerView
    private lateinit var recyclerConversations: RecyclerView



    // Chat Views
    private lateinit var btnChatBack: ImageButton
    private lateinit var chatUserAvatar: ImageView
    private lateinit var chatHeaderTitle: TextView
    private lateinit var chatHeaderSubtitle: TextView
    private lateinit var btnChatOptions: ImageButton
    private lateinit var recyclerChatMessages: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendMessage: ImageButton

    // Adapters & State
    private lateinit var inboxAdapter: InboxConversationAdapter
    private lateinit var inlineSearchAdapter: UserChatSearchAdapter
    private var chatAdapter: ChatAdapter? = null

    private var currentFilter = InboxConversationAdapter.FilterType.ALL
    private var currentSearchQuery = ""
    private var activeChatUser: String? = null
    private var blockedUserIds: Set<String> = emptySet()
    private var blockedUsernames: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        bindViews()
        setupAdapters()
        setupSearch()
        setupTabs()
        setupActions()
        setupBottomNav()
        setupChat()
        setupBackNavigation()

        val requestedChatUser = intent.getStringExtra("open_user")
        fetchRealAppUsersFromFirestore { users ->
            requestedChatUser?.let { requested ->
                val resolvedUser = users.firstOrNull { it.equals(requested, ignoreCase = true) } ?: requested
                openChat(resolvedUser)
            }
        }
        refreshInboxData()
    }



    private fun fetchRealAppUsersFromFirestore(onLoaded: ((List<String>) -> Unit)? = null) {
        val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (e: Exception) { null }
        val currentUid = auth?.currentUser?.uid

        TrustSafetyManager.getBlockedUsers { blockedUsers ->
            blockedUserIds = blockedUsers.mapTo(mutableSetOf()) { it.uid }
            blockedUsernames = blockedUsers.map { it.username.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val userList = mutableListOf<String>()
                        for (doc in querySnapshot) {
                            val uid = doc.id
                            if (uid == currentUid || uid in blockedUserIds) continue
                            val username = doc.getString("username")
                                ?: doc.getString("fullName")
                                ?: doc.getString("name")
                                ?: ""
                            if (username.isNotBlank() && username.lowercase() !in blockedUsernames) {
                                userList.add(username)
                                MessageManager.registerUser(username)
                            }
                        }
                        val sortedUsers = userList.distinct().sorted()
                        inlineSearchAdapter.updateUsers(sortedUsers)
                        onLoaded?.invoke(sortedUsers)
                        refreshInboxData()
                        if (activeChatUser?.lowercase()?.let { it in blockedUsernames } == true) closeChat()
                    }
                    .addOnFailureListener {
                        val allowedUsers = getAllKnownUsers().filter { it.lowercase() !in blockedUsernames }
                        inlineSearchAdapter.updateUsers(allowedUsers)
                        onLoaded?.invoke(allowedUsers)
                        refreshInboxData()
                        if (activeChatUser?.lowercase()?.let { it in blockedUsernames } == true) closeChat()
                    }
            } catch (_: Exception) {
                val allowedUsers = getAllKnownUsers().filter { it.lowercase() !in blockedUsernames }
                onLoaded?.invoke(allowedUsers)
            }
        }
    }

    private fun bindViews() {
        inboxContainer = findViewById(R.id.inboxContainer)
        chatContainer = findViewById(R.id.chatContainer)

        btnInboxNotifications = findViewById(R.id.btnInboxNotifications)
        btnInboxProfile = findViewById(R.id.btnInboxProfile)
        searchMessagesInput = findViewById(R.id.searchMessagesInput)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        btnNewChat = findViewById(R.id.btnNewChat)
        tabAll = findViewById(R.id.tabAll)
        tabUnread = findViewById(R.id.tabUnread)
        tabGroups = findViewById(R.id.tabGroups)
        cardFriendsRequests = findViewById(R.id.cardFriendsRequests)
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
        tvSearchUsersHeader = findViewById(R.id.tvSearchUsersHeader)
        recyclerSearchUsers = findViewById(R.id.recyclerSearchUsers)
        recyclerConversations = findViewById(R.id.recyclerConversations)



        btnChatBack = findViewById(R.id.btnChatBack)
        chatUserAvatar = findViewById(R.id.chatUserAvatar)
        chatHeaderTitle = findViewById(R.id.chatHeaderTitle)
        chatHeaderSubtitle = findViewById(R.id.chatHeaderSubtitle)
        btnChatOptions = findViewById(R.id.btnChatOptions)
        recyclerChatMessages = findViewById(R.id.recyclerChatMessages)
        messageInput = findViewById(R.id.messageInput)
        sendMessage = findViewById(R.id.sendMessage)
    }

    private fun setupAdapters() {
        // Inbox Conversations
        inboxAdapter = InboxConversationAdapter(
            MessageManager.getConversationsList()
        ) { conversation ->
            openChat(conversation.name)
        }
        recyclerConversations.layoutManager = LinearLayoutManager(this)
        recyclerConversations.adapter = inboxAdapter

        // Inline User Search
        val allUsers = getAllKnownUsers()
        inlineSearchAdapter = UserChatSearchAdapter(allUsers) { selectedUser ->
            openChat(selectedUser)
        }
        recyclerSearchUsers.layoutManager = LinearLayoutManager(this)
        recyclerSearchUsers.adapter = inlineSearchAdapter
    }

    private fun getAllKnownUsers(): List<String> {
        val all = mutableSetOf<String>()
        all.addAll(MessageManager.getAllUsernames())
        all.addAll(UserManager.getAllUsernames())
        return all.filter { it.isNotBlank() && !it.equals("Me", ignoreCase = true) }.sorted()
    }

    private fun setupSearch() {
        searchMessagesInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                currentSearchQuery = query

                if (query.isNotEmpty()) {
                    btnClearSearch.visibility = View.VISIBLE
                    tvSearchUsersHeader.visibility = View.VISIBLE
                    recyclerSearchUsers.visibility = View.VISIBLE
                    inlineSearchAdapter.filter(query)
                } else {
                    btnClearSearch.visibility = View.GONE
                    tvSearchUsersHeader.visibility = View.GONE
                    recyclerSearchUsers.visibility = View.GONE
                }

                inboxAdapter.applyFilter(currentFilter, currentSearchQuery)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnClearSearch.setOnClickListener {
            searchMessagesInput.text.clear()
        }
    }

    private fun setupTabs() {
        tabAll.setOnClickListener {
            selectTab(InboxConversationAdapter.FilterType.ALL)
        }

        tabUnread.setOnClickListener {
            selectTab(InboxConversationAdapter.FilterType.UNREAD)
        }

        tabGroups.setOnClickListener {
            selectTab(InboxConversationAdapter.FilterType.GROUPS)
        }
    }

    private fun selectTab(filterType: InboxConversationAdapter.FilterType) {
        currentFilter = filterType

        tabAll.setBackgroundResource(
            if (filterType == InboxConversationAdapter.FilterType.ALL) R.drawable.pill_tab_active else R.drawable.pill_tab_inactive
        )
        tabAll.setTextColor(
            getColor(if (filterType == InboxConversationAdapter.FilterType.ALL) R.color.circuit_lime else R.color.circuit_text_secondary)
        )

        tabUnread.setBackgroundResource(
            if (filterType == InboxConversationAdapter.FilterType.UNREAD) R.drawable.pill_tab_active else R.drawable.pill_tab_inactive
        )
        tabUnread.setTextColor(
            getColor(if (filterType == InboxConversationAdapter.FilterType.UNREAD) R.color.circuit_lime else R.color.circuit_text_secondary)
        )

        tabGroups.setBackgroundResource(
            if (filterType == InboxConversationAdapter.FilterType.GROUPS) R.drawable.pill_tab_active else R.drawable.pill_tab_inactive
        )
        tabGroups.setTextColor(
            getColor(if (filterType == InboxConversationAdapter.FilterType.GROUPS) R.color.circuit_lime else R.color.circuit_text_secondary)
        )

        inboxAdapter.applyFilter(currentFilter, currentSearchQuery)
    }

    private fun setupActions() {
        btnInboxNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        btnInboxProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        cardFriendsRequests.setOnClickListener {
            startActivity(Intent(this, FriendsActivity::class.java))
        }

        btnMarkAllRead.setOnClickListener {
            MessageManager.markAllAsRead()
            refreshInboxData()
        }

        btnNewChat.setOnClickListener {
            showNewChatDialog()
        }
    }

    private fun showNewChatDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_chat_search, null)
        val closeBtn = dialogView.findViewById<ImageButton>(R.id.btnCloseNewChatDialog)
        val searchInput = dialogView.findViewById<EditText>(R.id.dialogSearchUserInput)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.dialogUsersRecyclerView)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val allUsers = getAllKnownUsers()
        val dialogAdapter = UserChatSearchAdapter(allUsers) { selectedUser ->
            dialog.dismiss()
            openChat(selectedUser)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = dialogAdapter

        fetchRealAppUsersFromFirestore { users ->
            dialogAdapter.updateUsers(users)
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                dialogAdapter.filter(s?.toString()?.trim() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupBottomNav() {
        setupStandardBottomNav(BottomTab.MESSAGES)
    }

    private fun setupChat() {
        btnChatBack.setOnClickListener {
            closeChat()
        }

        btnChatOptions.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menu.add("Clear History")
            popup.menu.add("View Profile")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "View Profile" -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }
                    "Clear History" -> {
                        activeChatUser?.let { user ->
                            MessageManager.getMessagesForUser(user).clear()
                            chatAdapter?.notifyDataSetChanged()
                            refreshInboxData()
                        }
                    }
                }
                true
            }
            popup.show()
        }

        sendMessage.setOnClickListener {
            sendCurrentMessage()
        }
    }

    private fun sendCurrentMessage() {
        val user = activeChatUser ?: return
        if (user.lowercase() in blockedUsernames) {
            Toast.makeText(this, "Unblock this player before messaging", Toast.LENGTH_SHORT).show()
            closeChat()
            return
        }
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        val newMessage = Message("Me", text)
        MessageManager.sendMessageForUser(user, newMessage)

        val messages = MessageManager.getMessagesForUser(user)
        chatAdapter?.notifyItemInserted(messages.size - 1)
        recyclerChatMessages.scrollToPosition(messages.size - 1)

        messageInput.text.clear()
        refreshInboxData()
    }

    private fun openChat(user: String) {
        if (user.lowercase() in blockedUsernames) {
            Toast.makeText(this, "This player is blocked", Toast.LENGTH_SHORT).show()
            return
        }
        activeChatUser = user
        MessageManager.markAsRead(user)

        chatHeaderTitle.text = user
        chatHeaderSubtitle.text = "Active now"

        val messages = MessageManager.getMessagesForUser(user)
        chatAdapter = ChatAdapter(messages)
        recyclerChatMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerChatMessages.adapter = chatAdapter

        if (messages.isNotEmpty()) {
            recyclerChatMessages.scrollToPosition(messages.size - 1)
        }

        inboxContainer.visibility = View.GONE
        chatContainer.visibility = View.VISIBLE
    }

    private fun closeChat() {
        activeChatUser = null
        chatContainer.visibility = View.GONE
        inboxContainer.visibility = View.VISIBLE
        refreshInboxData()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (chatContainer.visibility == View.VISIBLE) {
                    closeChat()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun refreshInboxData() {
        val conversations = MessageManager.getConversationsList()
            .filter { it.name.lowercase() !in blockedUsernames }
        inboxAdapter.updateData(conversations)
        inboxAdapter.applyFilter(currentFilter, currentSearchQuery)

        val totalUnread = MessageManager.getTotalUnreadCount()
        tabUnread.text = if (totalUnread > 0) "Unread · $totalUnread" else "Unread"
    }

    override fun onResume() {
        super.onResume()
        fetchRealAppUsersFromFirestore()
        refreshInboxData()
        activeChatUser?.let { user ->
            val messages = MessageManager.getMessagesForUser(user)
            chatAdapter?.notifyDataSetChanged()
            if (messages.isNotEmpty()) {
                recyclerChatMessages.scrollToPosition(messages.size - 1)
            }
        }
    }
}
