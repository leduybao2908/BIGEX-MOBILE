package com.example.dacs3.ui.screens.SocialNetwork.model

//import org.w3c.dom.Comment

data class Post(
    val id: String = "",
    val caption: String = "",
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String? = null,
        val reactions: Map<String, String> = mapOf(), // Thêm dòng này
    val comments: Map<String, Comment> = mapOf() // 👈 Đổi từ List sang Map
    )
data class Comment(
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)
data class ReactionInfo(
    val userId: String = "",
    val userName: String = "",
    val avatar: String = "",
    val type: String = "",
    val timestamp: Long = 0L
)



