package com.example.dacs3.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dacs3.MainActivity
import com.example.dacs3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: ""
            val body = remoteMessage.data["body"] ?: ""
            val senderId = remoteMessage.data["senderId"]
            val senderName = remoteMessage.data["senderName"]
            val messageId = remoteMessage.data["messageId"]

            // Create notification title using sender's name if available
            val notificationTitle = if (!senderName.isNullOrEmpty()) {
                "Message from $senderName"
            } else {
                title
            }

            // Send notification with the message data and extras
            sendNotification(
                title = notificationTitle,
                messageBody = body,
                senderId = senderId,
                messageId = messageId
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        // Store the token in Firebase Realtime Database
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance()
            val userTokenRef = database.getReference("user_tokens").child(currentUser.uid)
            userTokenRef.setValue(token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token successfully updated in database")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update FCM token in database", e)
                }
        }
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        senderId: String?,
        messageId: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Add data for deep linking to message screen
            data = android.net.Uri.parse("android-app://androidx.navigation/message/${senderId}/Unknown")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "chat_messages"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}