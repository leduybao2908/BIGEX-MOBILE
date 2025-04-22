package com.example.dacs3.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dacs3.service.NotificationService

class TreeViewModelFactory(
    private val context: Context,
    private val userId: String,
    private val notificationService: NotificationService = NotificationService(context) // Pass context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TreeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TreeViewModel(context, userId, notificationService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}