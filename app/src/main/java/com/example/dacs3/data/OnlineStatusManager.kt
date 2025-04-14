package com.example.dacs3.data

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OnlineStatusManager(private val userDatabase: UserDatabase) {
    private val auth = FirebaseAuth.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val activityLifecycleCallback = object : Application.ActivityLifecycleCallbacks {
        private var activityReferences = 0
        private var isActivityChangingConfigurations = false

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {
            if (activityReferences == 0 && !isActivityChangingConfigurations) {
                // App went to foreground
                coroutineScope.launch {
                    updateOnlineStatus(true)
                }
            }
            activityReferences++
        }

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
            isActivityChangingConfigurations = activity.isChangingConfigurations
            activityReferences--
            if (activityReferences == 0 && !isActivityChangingConfigurations) {
                // App went to background
                coroutineScope.launch {
                    updateOnlineStatus(false)
                }
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }

    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(activityLifecycleCallback)
    }

    private suspend fun updateOnlineStatus(isOnline: Boolean) {
        auth.currentUser?.uid?.let { uid ->
            userDatabase.updateUserStatus(uid, isOnline)
        }
    }

    suspend fun cleanup(application: Application) {
        coroutineScope.launch {
            application.unregisterActivityLifecycleCallbacks(activityLifecycleCallback)
            updateOnlineStatus(false)
        }
    }
}