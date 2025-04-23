package com.example.dacs3.screens

import android.Manifest
import android.content.*
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.agora.rtc2.*

@Composable
fun VoiceCallScreen(
    friendId: String,
    friendUsername: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isJoined by remember { mutableStateOf(false) }
    var mRtcEngine: RtcEngine? by remember { mutableStateOf(null) }
    val myAppId = "a426b745d3d942f5857991c198e2a684"
    val channelName = "test_channel"
    val token = "007eJxTYHCY8tHDJPy0ZLcIg4L2o7IZHJfv/+Dg8G0rtYz946ugs0SBIdHEyCzJ3MQ0xTjF0sQozdTC1NzS0jDZ0NIi1SjRzMIkdgN7RkMgI4PQHxVmRgYIBPF5GEpSi0vikzMS8/JScxgYAEN8Huc="

    val mRtcEventHandler = remember {
        object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                Toast.makeText(context, "Đã tham gia kênh $channel", Toast.LENGTH_SHORT).show()
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                Toast.makeText(context, "Người dùng đã tham gia", Toast.LENGTH_SHORT).show()
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                Toast.makeText(context, "Người dùng đã rời kênh: $uid", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startVoiceCalling(context, mRtcEngine, mRtcEventHandler, myAppId, channelName, token)
            isJoined = true
        } else {
            Toast.makeText(context, "Quyền truy cập bị từ chối", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isJoined) "Đang gọi với $friendUsername" else "Đang kết nối...",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (!isJoined) {
                    if (hasPermissions(context)) {
                        startVoiceCalling(context, mRtcEngine, mRtcEventHandler, myAppId, channelName, token)
                        isJoined = true
                    } else {
                        requestPermissions(permissionLauncher)
                    }
                } else {
                    mRtcEngine?.leaveChannel()
                    mRtcEngine?.stopPreview()
                    mRtcEngine = null
                    isJoined = false
                }
            }
        ) {
            Text(if (isJoined) "Rời Kênh" else "Tham Gia Kênh")
        }

        if (isJoined) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Đang trong cuộc gọi...")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mRtcEngine?.leaveChannel()
            mRtcEngine?.stopPreview()
            RtcEngine.destroy()
        }
    }
}

private fun hasPermissions(context: Context): Boolean {
    return getRequiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

private fun requestPermissions(launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>) {
    launcher.launch(getRequiredPermissions())
}

private fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }
}

private fun startVoiceCalling(
    context: Context,
    mRtcEngine: RtcEngine?,
    mRtcEventHandler: IRtcEngineEventHandler,
    appId: String,
    channelName: String,
    token: String
) {
    try {
        val config = RtcEngineConfig().apply {
            mContext = context
            mAppId = appId
            mEventHandler = mRtcEventHandler
        }
        val engine = RtcEngine.create(config)

        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishMicrophoneTrack = true
        }
        engine.joinChannel(token, channelName, 0, options)

    } catch (e: Exception) {
        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}