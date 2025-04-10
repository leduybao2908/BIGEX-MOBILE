package com.example.dacs3.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dacs3.agora.AgoraManager

class VideoCallViewModelFactory(
    private val application: Application,
    private val agoraManager: AgoraManager = AgoraManager(application)
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoCallViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoCallViewModel(application, agoraManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}