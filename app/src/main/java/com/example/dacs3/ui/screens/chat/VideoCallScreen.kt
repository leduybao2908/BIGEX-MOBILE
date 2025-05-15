package com.example.dacs3.ui.screens.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.launch

// AgoraManager to encapsulate Agora RTC logic
class AgoraManager(private val context: Context, private val appId: String) {
    private var rtcEngine: RtcEngine? = null

    fun initialize(handler: IRtcEngineEventHandler) {
        try {
            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = appId
                mEventHandler = handler
            }
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.enableVideo()
            rtcEngine?.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                )
            )
        } catch (e: Exception) {
            Log.e("AgoraManager", "Error initializing Agora: ${e.message}")
        }
    }

    fun joinChannel(token: String, channelName: String, uid: Int = 0) {
        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        }
        rtcEngine?.joinChannel(token, channelName, uid, options)
    }

    fun setupLocalVideo(surfaceView: SurfaceView) {
        surfaceView.setZOrderMediaOverlay(true)
        rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        rtcEngine?.startPreview()
    }

    fun setupRemoteVideo(surfaceView: SurfaceView, uid: Int) {
        surfaceView.setZOrderMediaOverlay(true)
        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }

    fun leaveChannel() {
        rtcEngine?.leaveChannel()
        rtcEngine?.stopPreview()
    }

    fun release() {
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }

    fun toggleLocalVideo(enable: Boolean) {
        rtcEngine?.enableLocalVideo(enable)
    }

    fun toggleLocalAudio(enable: Boolean) {
        rtcEngine?.enableLocalAudio(enable)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallScreen(

    viewModel: VideoCallViewModel) {
    val context = LocalContext.current
    var rtcEngine by remember { mutableStateOf<RtcEngine?>(null) }
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    val token = "007eJxTYDjFGzvzW5DSt8Tdqtd8rl4Uyl8r/GCXkMMZA215jiW3tv1SYDAzMUxJMUgzTzQ0SzZJTDFMMjEyNrY0T00xTjEyMEpNnSmjlNEQyMhgH3yPhZEBAkF8HoaS1OKS+OSMxLy81BwGBgBd+CJg"
    val appId = "641dd0f7a16c4ad1b423397ed3d202ee"
    val channelName = "test_channel"

    val handler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d("Agora", "Joined channel: $channel")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            val remoteSurface = SurfaceView(context).apply {
                setZOrderMediaOverlay(false) // Không đè lên local
            }
            rtcEngine?.setupRemoteVideo(VideoCanvas(remoteSurface, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            viewModel.remoteVideo = remoteSurface
        }


        override fun onUserOffline(uid: Int, reason: Int) {
            viewModel.remoteVideo = null
        }
    }

    fun startVideoCall() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = appId
                mEventHandler = handler
            }

            rtcEngine = RtcEngine.create(config).apply {
                enableVideo()
                setVideoEncoderConfiguration(
                    VideoEncoderConfiguration(
                        VideoEncoderConfiguration.VD_640x360,
                        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                        VideoEncoderConfiguration.STANDARD_BITRATE,
                        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                    )
                )
            }

            // Local video
            val surface = SurfaceView(context).apply {
                setZOrderMediaOverlay(true) // Đảm bảo hiển thị trên remote
            }
            localSurfaceView = surface
            viewModel.localVideo = surface

            rtcEngine?.setupLocalVideo(VideoCanvas(surface, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            rtcEngine?.startPreview()

            val options = ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                publishCameraTrack = true
                publishMicrophoneTrack = true
            }

            rtcEngine?.joinChannel(token, channelName, 0, options)
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            startVideoCall()
            viewModel.isJoined = true
        } else {
            Toast.makeText(context, "Cần quyền camera và micro", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasCamera && hasMic) {
            startVideoCall()
            viewModel.isJoined = true
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            rtcEngine?.leaveChannel()
            rtcEngine?.stopPreview()
            rtcEngine = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Remote video ở dưới
        viewModel.remoteVideo?.let { remoteView ->
            AndroidView(
                factory = { remoteView },
                modifier = Modifier
                    .fillMaxSize()
            )
        }

        // Local video hiển thị nhỏ ở góc
        viewModel.localVideo?.let { localView ->
            AndroidView(
                factory = { localView },
                modifier = Modifier
                    .size(120.dp, 160.dp)
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }

        // Button Join/Leave
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                if (viewModel.isJoined) {
                    rtcEngine?.leaveChannel()
                    rtcEngine?.stopPreview()
                    viewModel.isJoined = false
                    viewModel.remoteVideo = null
                    viewModel.localVideo = null
                } else {
                    startVideoCall()
                    viewModel.isJoined = true
                }
            }) {
                Text(if (viewModel.isJoined) "Leave Channel" else "Join Channel")
            }
        }
    }

}