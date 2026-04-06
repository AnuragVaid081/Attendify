package com.attendify.android.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.attendify.shared.repository.AuthRepository
import org.koin.android.ext.android.inject

class AttendifyFcmService : FirebaseMessagingService() {

    private val authRepository: AuthRepository by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Update FCM token in Firestore for push notification targeting
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            kotlinx.coroutines.GlobalScope.kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.IO) {
                authRepository.updateFcmToken(userId, token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Notification is auto-displayed by Firebase SDK when app is in background.
        // For foreground, you can show a local notification here.
        val title = message.notification?.title ?: message.data["title"] ?: "Attendify"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        showLocalNotification(title, body)
    }

    private fun showLocalNotification(title: String, body: String) {
        val channelId = "attendify_notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Create channel (Android 8+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, "Attendify Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
