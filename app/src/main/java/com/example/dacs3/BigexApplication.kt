package com.example.dacs3

import android.app.*
import com.example.dacs3.data.*
import com.example.dacs3.service.*
import com.zegocloud.uikit.prebuilt.call.*
import kotlinx.coroutines.*

class BigexApplication : Application() {
    private lateinit var onlineStatusManager: OnlineStatusManager
   
    override fun onCreate() {
        super.onCreate()
        
        // Initialize OnlineStatusManager
        onlineStatusManager = OnlineStatusManager(UserDatabase())
        onlineStatusManager.initialize(this)

        // Initialize NotificationService
        NotificationService.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        // Launch cleanup in a coroutine scope since it's a suspend function
        CoroutineScope(Dispatchers.Main).launch {
            onlineStatusManager.cleanup(this@BigexApplication)
        }
    }
}