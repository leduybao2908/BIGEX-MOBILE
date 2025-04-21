package com.example.dacs3

import android.app.Application
import com.example.dacs3.data.OnlineStatusManager
import com.example.dacs3.data.UserDatabase
import com.example.dacs3.service.NotificationService
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