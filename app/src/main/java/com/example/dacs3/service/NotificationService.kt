package com.example.dacs3.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dacs3.R
import com.example.dacs3.data.*
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.UUID

class NotificationService(private val context: Context) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val TAG = "NotificationService"
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "tree_watering_channel"

    companion object {
        private var credentials: GoogleCredentials? = null

        fun initialize(context: Context) {
            if (credentials == null) {
                context.assets.open("dacs3-5cf79-171f102451e7.json").use { stream ->
                    credentials = GoogleCredentials.fromStream(stream)
                        .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
                }
            }
        }
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tree Watering Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Send local notification
    fun showLocalNotification(title: String, body: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.seed) // Use an existing drawable or add ic_tree
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(UUID.randomUUID().hashCode(), notification)
    }

    // Send FCM notification
    suspend fun sendMessageNotification(
        token: String,
        title: String,
        body: String,
        senderId: String,
        senderName: String,
        messageId: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                val accessToken = credentials?.refreshAccessToken()?.tokenValue
                    ?: throw IllegalStateException("NotificationService not initialized. Call initialize() first.")

                val message = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", token)
                        put("notification", JSONObject().apply {
                            put("title", title)
                            put("body", body)
                        })
                        put("data", JSONObject().apply {
                            put("senderId", senderId)
                            put("senderName", senderName)
                            put("messageId", messageId)
                            put("title", title)
                            put("body", body)
                        })
                    })
                }

                val request = Request.Builder()
                    .url("https://fcm.googleapis.com/v1/projects/dacs3-5cf79/messages:send")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(message.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        Log.e(TAG, "FCM API error: ${response.code}, $errorBody")
                        throw IOException("FCM API error: ${response.code}")
                    } else {
                        Log.d(TAG, "Notification sent successfully")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send notification", e)
                throw e
            }
        }
    }

    // Schedule daily reminder
    fun scheduleDailyReminder(hour: Int, minute: Int, title: String, body: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WateringReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("body", body)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            UUID.randomUUID().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        fun sendNotification(
            token: String,
            title: String,
            body: String,
            data: Map<String, String?>
        ) {
            val json = JSONObject().apply {
                put("to", token)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                })
                put("data", JSONObject(data))
            }

            val requestBody = json.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(FirebaseConfig.DATABASE_URL)
                .post(requestBody)
                .addHeader("Authorization", "key=${FirebaseConfig.FCM_SERVER_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failed to send notification: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e(
                            TAG,
                            "Notification failed: ${response.code}, ${response.body?.string()}"
                        )
                    } else {
                        Log.d(TAG, "Notification sent successfully")
                    }
                }
            })
        }
    }

    class WateringReminderReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val notificationService = NotificationService(context)
            val title = intent.getStringExtra("title") ?: "Nhắc nhở tưới cây"
            val body = intent.getStringExtra("body") ?: "Đã đến giờ tưới cây!"
            notificationService.showLocalNotification(title, body)
        }
    }
}