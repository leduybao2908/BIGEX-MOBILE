package com.example.dacs3.ui.screens.chat

import android.view.SurfaceView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class VideoCallViewModel : ViewModel() {
    var localVideo: SurfaceView? by mutableStateOf(null)
    var remoteVideo: SurfaceView? by mutableStateOf(null)
    var isJoined: Boolean by mutableStateOf(false)
}