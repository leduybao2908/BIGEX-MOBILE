package com.example.dacs3.viewmodels

import android.app.Application
import android.view.SurfaceView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.agora.AgoraManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoCallViewModel(
    application: Application,
    private val agoraManager: AgoraManager = AgoraManager(application)
) : AndroidViewModel(application) {
    private val _isMicrophoneEnabled = MutableStateFlow(true)
    val isMicrophoneEnabled: StateFlow<Boolean> = _isMicrophoneEnabled.asStateFlow()

    private val _isCameraEnabled = MutableStateFlow(true)
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _isInCall = MutableStateFlow(false)
    val isInCall: StateFlow<Boolean> = _isInCall.asStateFlow()

    val isJoined = agoraManager.isJoined
    val remoteUserJoined = agoraManager.remoteUserJoined

    init {
        // Khởi tạo Agora Engine với App ID từ config
        agoraManager.initialize(com.example.dacs3.agora.AgoraConfig.APP_ID)
    }

    fun startRandomCall() {
        viewModelScope.launch {
            try {
                // Tạo một channel name ngẫu nhiên với prefix để dễ quản lý
                val channelName = "random_${System.currentTimeMillis()}_${(1000..9999).random()}"
                
                // Sử dụng temporary token cho testing
                // Trong production, token nên được lấy từ server
                val tempToken = "007eJxTYLhxZPGBxLXXbp5YtPXQxvVXjFbNPnRm+4Ij+Vc+Xz+6YPFiBQZLCzNTYxNjS1MLU3MTc0tTM0sL0yRjS8skS0uTJEuLtKVfUhoCGRkY/RkZGBkgEMRnYchNzMxjYAAAcwQfBA=="
                
                // Thử kết nối với channel
                agoraManager.joinChannel(tempToken, channelName)
                _isInCall.value = true
            } catch (e: Exception) {
                // Log lỗi và xử lý thất bại
                android.util.Log.e("VideoCallViewModel", "Failed to start call: ${e.message}")
                _isInCall.value = false
            }
        }
    }

    fun endCall() {
        viewModelScope.launch {
            agoraManager.leaveChannel()
            _isInCall.value = false
        }
    }

    fun toggleMicrophone() {
        _isMicrophoneEnabled.value = !_isMicrophoneEnabled.value
        agoraManager.toggleMicrophone(_isMicrophoneEnabled.value)
    }

    fun toggleCamera() {
        _isCameraEnabled.value = !_isCameraEnabled.value
        agoraManager.toggleCamera(_isCameraEnabled.value)
    }

    fun getLocalVideoView(): SurfaceView? = agoraManager.getLocalVideoView()
    fun getRemoteVideoView(): SurfaceView? = agoraManager.getRemoteVideoView()

    override fun onCleared() {
        super.onCleared()
        agoraManager.release()
    }
}