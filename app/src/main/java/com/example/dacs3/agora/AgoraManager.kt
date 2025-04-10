package com.example.dacs3.agora

import android.content.Context
import android.view.SurfaceView
import io.agora.rtc2.*
import io.agora.rtc2.video.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AgoraManager(private val context: Context) {
    private var agoraEngine: RtcEngine? = null
    private var currentChannel: String? = null
    private var currentToken: String? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    private val _isJoined = MutableStateFlow(false)
    val isJoined: StateFlow<Boolean> = _isJoined

    private val _remoteUserJoined = MutableStateFlow(false)
    val remoteUserJoined: StateFlow<Boolean> = _remoteUserJoined

    private val _isLocalVideoReady = MutableStateFlow(false)
    val isLocalVideoReady: StateFlow<Boolean> = _isLocalVideoReady

    private val _isRemoteVideoReady = MutableStateFlow(false)
    val isRemoteVideoReady: StateFlow<Boolean> = _isRemoteVideoReady

    fun initialize(appId: String) {
        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = appId
            config.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    _isJoined.value = true
                    // Ensure local video view is set up immediately after joining
                    setupLocalVideo()
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    _remoteUserJoined.value = true
                    // Ensure remote video view is set up when user joins
                    setupRemoteVideo(uid)
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    _remoteUserJoined.value = false
                    remoteSurfaceView?.let { view ->
                        view.holder.removeCallback(remoteCallback)
                        view.visibility = android.view.View.GONE
                    }
                    remoteSurfaceView = null
                    // Try to find another user if current user leaves
                    tryFindNewUser()
                }

                override fun onConnectionStateChanged(state: Int, reason: Int) {
                    when (state) {
                        Constants.CONNECTION_STATE_FAILED -> {
                            _isJoined.value = false
                            _remoteUserJoined.value = false
                            if (reason == Constants.CONNECTION_CHANGED_INTERRUPTED) {
                                tryReconnect()
                            }
                        }
                        Constants.CONNECTION_STATE_CONNECTING -> {
                            android.util.Log.d("AgoraManager", "Connecting to channel...")
                        }
                        Constants.CONNECTION_STATE_CONNECTED -> {
                            // Ensure video views are set up when connection is established
                            setupLocalVideo()
                        }
                    }
                }

                override fun onFirstLocalVideoFrame(
                    source: Constants.VideoSourceType?,
                    width: Int,
                    height: Int,
                    elapsed: Int
                ) {
                    _isLocalVideoReady.value = true
                    android.util.Log.d("AgoraManager", "First local video frame rendered")
                }

                override fun onFirstRemoteVideoFrame(uid: Int, width: Int, height: Int, elapsed: Int) {
                    _isRemoteVideoReady.value = true
                    android.util.Log.d("AgoraManager", "First remote video frame rendered")
                }
            }
            agoraEngine = RtcEngine.create(config)
            agoraEngine?.enableVideo()
            
            // Configure video parameters for better performance
            agoraEngine?.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                )
            )
            
            // Cấu hình thêm cho strategy chain và pathfinder
            agoraEngine?.setParameters("{\"rtc.video.playout_delay_margin\":200}")
            agoraEngine?.setParameters("{\"che.video.local.camera_index\":1}")
            agoraEngine?.setParameters("{\"che.video.local.preference_width\":640}")
            agoraEngine?.setParameters("{\"che.video.local.preference_height\":360}")
            agoraEngine?.setParameters("{\"che.video.lowbitrate\":400}")
            agoraEngine?.setParameters("{\"rtc.enable_agc\":true}")
            agoraEngine?.setParameters("{\"che.audio.enable_agc\":true}")
            agoraEngine?.setParameters("{\"che.audio.enable_ns\":true}")
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize Agora RTC engine", e)
        }
    }

    fun joinChannel(token: String, channelName: String, uid: Int = 0) {
        currentToken = token
        currentChannel = channelName
        if (setupLocalVideo()) {
            agoraEngine?.startPreview()
            agoraEngine?.joinChannel(token, channelName, "", uid)
        }
    }

    fun leaveChannel() {
        agoraEngine?.leaveChannel()
        _isJoined.value = false
        _remoteUserJoined.value = false
        _isLocalVideoReady.value = false
        _isRemoteVideoReady.value = false

        localSurfaceView?.let { view ->
            view.holder.removeCallback(localCallback)
            view.visibility = android.view.View.GONE
        }
        localSurfaceView = null

        remoteSurfaceView?.let { view ->
            view.holder.removeCallback(remoteCallback)
            view.visibility = android.view.View.GONE
        }
        remoteSurfaceView = null
    }

    fun toggleMicrophone(enabled: Boolean) {
        agoraEngine?.muteLocalAudioStream(!enabled)
    }

    fun toggleCamera(enabled: Boolean) {
        agoraEngine?.muteLocalVideoStream(!enabled)
    }

    private val localCallback = object : android.view.SurfaceHolder.Callback {
        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
            _isLocalVideoReady.value = true
            holder.setKeepScreenOn(true)
        }
        
        override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
        
        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
            _isLocalVideoReady.value = false
            holder.setKeepScreenOn(false)
        }
    }

    private val remoteCallback = object : android.view.SurfaceHolder.Callback {
        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
            _isRemoteVideoReady.value = true
            holder.setKeepScreenOn(true)
        }
        
        override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
        
        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
            _isRemoteVideoReady.value = false
            holder.setKeepScreenOn(false)
        }
    }

    private fun setupLocalVideo(): Boolean {
        try {
            localSurfaceView = SurfaceView(context).apply {
                setZOrderMediaOverlay(true)
                visibility = android.view.View.VISIBLE
            }
            
            localSurfaceView?.holder?.addCallback(localCallback)

            localSurfaceView?.let { view ->
                agoraEngine?.setupLocalVideo(
                    VideoCanvas(
                        view,
                        VideoCanvas.RENDER_MODE_HIDDEN,
                        0
                    )
                )
                return true
            }
            return false
        } catch (e: Exception) {
            android.util.Log.e("AgoraManager", "Failed to setup local video: ${e.message}")
            return false
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        try {
            remoteSurfaceView = SurfaceView(context).apply {
                visibility = android.view.View.VISIBLE
            }
            
            remoteSurfaceView?.holder?.addCallback(remoteCallback)

            remoteSurfaceView?.let { view ->
                agoraEngine?.setupRemoteVideo(
                    VideoCanvas(
                        view,
                        VideoCanvas.RENDER_MODE_HIDDEN,
                        uid
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AgoraManager", "Failed to setup remote video: ${e.message}")
        }
    }

    fun getLocalVideoView(): SurfaceView? = localSurfaceView
    fun getRemoteVideoView(): SurfaceView? = remoteSurfaceView

    fun release() {
        leaveChannel()
        RtcEngine.destroy()
        agoraEngine = null
    }

    private fun tryFindNewUser() {
        // Attempt to find and connect with another available user in the channel
        android.util.Log.d("AgoraManager", "Attempting to find new user...")
        // The RTC engine will automatically handle new user discovery
        // through the onUserJoined callback
    }

    private fun tryReconnect() {
        android.util.Log.d("AgoraManager", "Connection interrupted, attempting to reconnect...")
        // Sử dụng thông tin đã lưu trữ
        val channel = currentChannel
        val token = currentToken
        
        if (channel != null && token != null) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                joinChannel(token, channel)
            }, 1000)
        }
    }
}