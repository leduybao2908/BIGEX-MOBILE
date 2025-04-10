package com.example.dacs3.service

import com.example.dacs3.data.FirebaseConfig
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

    suspend fun sendMessageNotification(
        token: String,
        title: String,
        body: String,
        senderId: String,
        senderName: String,
        messageId: String
    ) {
        withContext(Dispatchers.IO) {
            val notification = JSONObject().apply {
                put("title", title)
                put("body", body)
            }

            val data = JSONObject().apply {
                put("senderId", senderId)
                put("senderName", senderName)
                put("messageId", messageId)
                put("title", title)
                put("body", body)
            }

            val message = JSONObject().apply {
                put("to", token)
                put("notification", notification)
                put("data", data)
                put("priority", "high")
            }

            val request = Request.Builder()
                .url(FirebaseConfig.FCM_API_URL)
                .addHeader("Authorization", "key=${FirebaseConfig.FCM_SERVER_KEY}")
                .post(message.toString().toRequestBody(jsonMediaType))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected response code: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }
}