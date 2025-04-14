package com.example.dacs3.data

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val isImage: Boolean = false,
    val reactions: Map<String, String> = mapOf() // Map of userId to reaction emoji
)