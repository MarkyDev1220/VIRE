package com.vire.android.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object NotificationHelper {
    private const val CHANNEL_ID = "vire_notifications_channel"
    private const val CHANNEL_NAME = "VIRE Notifications"
    private const val CHANNEL_DESC = "Notifications for friend requests, comments, and challenges"

    private var listenerRegistration: ListenerRegistration? = null
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.description = CHANNEL_DESC
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showSystemNotification(context: Context, notificationId: Int, title: String, message: String) {
        ensureChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "POST_NOTIFICATIONS permission not granted: ${e.message}")
        }
    }

    fun sendNotification(
        toUid: String,
        fromUid: String,
        fromUsername: String,
        title: String,
        message: String,
        type: String,
        relatedId: String = ""
    ) {
        if (toUid.isEmpty() || toUid == fromUid) return

        val ref = db.collection("notifications").document()
        val data = hashMapOf(
            "id" to ref.id,
            "toUid" to toUid,
            "fromUid" to fromUid,
            "fromUsername" to fromUsername,
            "title" to title,
            "message" to message,
            "type" to type,
            "relatedId" to relatedId,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )

        ref.set(data)
            .addOnFailureListener { e ->
                Log.e("NotificationHelper", "Error sending notification: ${e.message}")
            }
    }

    fun listenForNotifications(context: Context) {
        val currentUid = auth.currentUser?.uid ?: return
        if (listenerRegistration != null) return

        listenerRegistration = db.collection("notifications")
            .whereEqualTo("toUid", currentUid)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                for (dc in snapshots.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val doc = dc.document
                        val id = doc.id
                        val title = doc.getString("title") ?: "VIRE Notification"
                        val message = doc.getString("message") ?: ""
                        val isRead = doc.getBoolean("read") ?: false
                        val type = doc.getString("type") ?: "GENERAL"
                        val fromUid = doc.getString("fromUid") ?: ""
                        val fromUsername = doc.getString("fromUsername") ?: ""
                        val relatedId = doc.getString("relatedId") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                        val item = NotificationItem(
                            id = id,
                            toUid = currentUid,
                            fromUid = fromUid,
                            fromUsername = fromUsername,
                            title = title,
                            message = message,
                            type = type,
                            relatedId = relatedId,
                            timestamp = timestamp,
                            read = isRead
                        )

                        NotificationStore.addNotification(item)

                        // Show system push banner for unread items
                        if (!isRead) {
                            showSystemNotification(context, id.hashCode(), title, message)
                        }
                    }
                }
            }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    fun markAsReadInFirestore(notificationId: String) {
        if (notificationId.isEmpty()) return
        db.collection("notifications").document(notificationId)
            .update("read", true)
            .addOnSuccessListener {
                NotificationStore.markAsRead(notificationId)
            }
    }
}
