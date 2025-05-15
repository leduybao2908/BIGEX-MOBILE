package com.example.dacs3.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.dacs3.MainActivity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import android.media.RingtoneManager
import android.media.Ringtone
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context

class VideoCallService : Service() {
    private val CHANNEL_ID = "video_call_channel"
    private val NOTIFICATION_ID = 1
    private val realtimeDb = Firebase.database
    private var currentUserId: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentUserId = intent?.getStringExtra("userId")
        listenForIncomingCalls()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Video Call Notifications"
            val descriptionText = "Thông báo cuộc gọi video"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 1000)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun listenForIncomingCalls() {
        currentUserId?.let { userId ->
            realtimeDb.reference.child("calls").child(userId)
                .addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        val callData = snapshot.value as? Map<*, *>
                        callData?.let {
                            val callerId = it["callerId"] as? String
                            val callerName = it["callerName"] as? String
                            if (callerId != null && callerName != null) {
                                showIncomingCallNotification(callerId, callerName)
                                playRingtone()
                                vibrate()
                            }
                        }
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        // Hủy thông báo khi cuộc gọi bị từ chối hoặc kết thúc
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(NOTIFICATION_ID)
                        stopRingtone()
                        stopVibrate()
                    }
                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun showIncomingCallNotification(callerId: String, callerName: String) {
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "accept_call")
            putExtra("callerId", callerId)
            putExtra("callerName", callerName)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            this, 0, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "decline_call")
            putExtra("callerId", callerId)
        }
        val declinePendingIntent = PendingIntent.getActivity(
            this, 1, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cuộc gọi video đến")
            .setContentText("Từ $callerName")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_call, "Chấp nhận", acceptPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Từ chối", declinePendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private var ringtone: Ringtone? = null

    private fun playRingtone() {
        if (ringtone == null) {
            ringtone = RingtoneManager.getRingtone(
                applicationContext,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            )
        }
        ringtone?.play()
    }

    private fun stopRingtone() {
        ringtone?.stop()
    }

    private var vibrator: Vibrator? = null

    private fun vibrate() {
        if (vibrator == null) {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 1000),
                    intArrayOf(0, 255, 0),
                    0
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 1000), 0)
        }
    }

    private fun stopVibrate() {
        vibrator?.cancel()
    }
}