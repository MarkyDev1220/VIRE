package com.vire.android.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.vire.android.R

class NotificationsActivity : BaseActivity() {

    private lateinit var recyclerNotifications: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnClearAll: TextView
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        setupStandardBottomNav(BottomTab.NONE)

        recyclerNotifications = findViewById(R.id.recyclerNotifications)
        tvEmpty = findViewById(R.id.tvEmptyNotifications)
        btnBack = findViewById(R.id.btnBackNotifications)
        btnClearAll = findViewById(R.id.btnClearAllNotifications)

        btnBack.setOnClickListener { finish() }

        btnClearAll.setOnClickListener {
            val list = NotificationStore.getAllNotifications()
            list.forEach { NotificationStore.markAsRead(it.id) }
            refreshList()
        }

        recyclerNotifications.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(emptyList()) { item ->
            NotificationStore.markAsRead(item.id)
            refreshList()
        }
        recyclerNotifications.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val uid = try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null } ?: ""
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val myUsername = prefs.getString("username", "") ?: ""

        val notifications = NotificationStore.getAllNotifications().filter {
            it.toUid == uid || it.toUser.equals(myUsername, ignoreCase = true) || it.toUser.isBlank()
        }

        if (notifications.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerNotifications.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerNotifications.visibility = View.VISIBLE
            adapter.updateItems(notifications)
        }
    }

    class NotificationAdapter(
        private var items: List<NotificationItem>,
        private val onItemClick: (NotificationItem) -> Unit
    ) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.ivNotificationIcon)
            val title: TextView = view.findViewById(R.id.tvNotificationTitle)
            val message: TextView = view.findViewById(R.id.tvNotificationMessage)
            val time: TextView = view.findViewById(R.id.tvNotificationTime)
            val unreadDot: View = view.findViewById(R.id.unreadDot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.message.text = item.message
            holder.unreadDot.visibility = if (item.read) View.GONE else View.VISIBLE

            val iconRes = when (item.type) {
                "FRIEND_REQUEST" -> R.drawable.ic_friends
                "COMMENT" -> R.drawable.ic_messages
                "CHALLENGE" -> R.drawable.ic_challenges
                else -> R.drawable.ic_messages
            }
            holder.icon.setImageResource(iconRes)

            val diff = System.currentTimeMillis() - item.timestamp
            val minutes = diff / (60 * 1000)
            val hours = minutes / 60
            holder.time.text = when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                else -> "${hours / 24}d ago"
            }

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<NotificationItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
