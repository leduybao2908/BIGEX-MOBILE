package com.example.dacs3.ui.screens.SocialNetwork.model

data class Post(
    val id: String = "",
    val caption: String = "",
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
