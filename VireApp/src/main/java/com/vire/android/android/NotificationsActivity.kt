package com.vire.android.android

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.vire.android.R
import java.text.DateFormat
import java.util.Date

class NotificationsActivity : BaseActivity() {
    private lateinit var notificationsList: LinearLayout
    private lateinit var emptyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        setupStandardBottomNav(BottomTab.NONE)
        notificationsList = findViewById(R.id.notificationsList)
        emptyMessage = findViewById(R.id.notificationsEmpty)
    }

    override fun onResume() {
        super.onResume()
        renderNotifications()
    }

    private fun renderNotifications() {
        notificationsList.removeAllViews()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val notifications = uid?.let(NotificationStore::getNotificationsForUser).orEmpty()
        emptyMessage.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE

        notifications.forEach { notification ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16.dp, 12.dp, 16.dp, 12.dp)
                setBackgroundColor(ContextCompat.getColor(this@NotificationsActivity, R.color.circuit_card))
            }
            val title = TextView(this).apply {
                text = notification.title
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@NotificationsActivity, R.color.circuit_text_primary))
            }
            val message = TextView(this).apply {
                text = notification.message
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@NotificationsActivity, R.color.circuit_text_secondary))
            }
            val timestamp = TextView(this).apply {
                text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(notification.timestamp))
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@NotificationsActivity, R.color.circuit_text_muted))
            }

            card.addView(title)
            card.addView(message, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4.dp })
            card.addView(timestamp, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8.dp })
            notificationsList.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8.dp })
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
