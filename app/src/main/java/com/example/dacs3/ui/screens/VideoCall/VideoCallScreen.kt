package com.example.dacs3.ui.screens.VideoCall

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.viewmodels.VideoCallViewModel
import com.example.dacs3.viewmodels.VideoCallViewModelFactory
import android.app.Application
import androidx.compose.ui.platform.LocalContext

@Composable
fun VideoCallScreen() {
    val context = LocalContext.current
    val viewModel: VideoCallViewModel = viewModel(
        factory = VideoCallViewModelFactory(context.applicationContext as Application)
    )
    var isInCall by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isInCall) {
            // Màn hình chờ
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Hiển thị preview camera của người dùng
                viewModel.getLocalVideoView()?.let { localView ->
                    AndroidView(
                        factory = { localView },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .padding(bottom = 16.dp)
                    )
                }

                Text(
                    text = "Bắt đầu cuộc gọi video với người lạ",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.startRandomCall()
                        isInCall = true
                    }
                ) {
                    Text("Bắt đầu")
                }
            }
        } else {
            // Màn hình cuộc gọi
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Video views
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val isJoined by viewModel.isJoined.collectAsState()
                    val remoteUserJoined by viewModel.remoteUserJoined.collectAsState()

                    if (!isJoined) {
                        Text("Đang kết nối...")
                    } else {
                        // Remote video (full screen)
                        viewModel.getRemoteVideoView()?.let { remoteView ->
                            AndroidView(
                                factory = { remoteView },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Local video (picture-in-picture)
                        viewModel.getLocalVideoView()?.let { localView ->
                            AndroidView(
                                factory = { localView },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .width(120.dp)
                                    .aspectRatio(0.75f)
                            )
                        }

                        if (!remoteUserJoined) {
                            Text(
                                "Đang chờ người dùng khác tham gia...",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                // Các nút điều khiển
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { viewModel.toggleMicrophone() }
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Toggle microphone")
                    }

                    IconButton(
                        onClick = { 
                            viewModel.endCall()
                            isInCall = false
                        }
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End call")
                    }

                    IconButton(
                        onClick = { viewModel.toggleCamera() }
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Toggle camera")
                    }
                }
            }
        }
    }
}