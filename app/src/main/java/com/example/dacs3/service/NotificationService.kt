package com.example.dacs3.service

import android.content.Context
import android.util.Log
import com.example.dacs3.data.FirebaseConfig
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class NotificationService {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val TAG = "NotificationService"

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
                // Get access token from cached credentials
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
}
