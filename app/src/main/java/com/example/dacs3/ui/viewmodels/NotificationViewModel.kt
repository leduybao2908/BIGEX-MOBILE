package com.example.dacs3.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.UserDatabase
import com.example.dacs3.data.UserDatabaseModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val fromUserId: String,
    val fromUsername: String,
    val timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    var profilePicture: String = ""
)

class NotificationViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val userDatabase = UserDatabase()
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        if (currentUserId.isEmpty()) return

        // Observe friend requests
        userDatabase.observeFriendRequests(currentUserId,
            onDataChange = { requests ->
                viewModelScope.launch {
                    val notificationsList = mutableListOf<Notification>()

                    requests.forEach { (fromUserId, _) ->
                        try {
                            val userSnapshot = userDatabase.getUserById(fromUserId)
                            val user = userSnapshot.getValue<UserDatabaseModel>()
                            if (user != null) {
                                notificationsList.add(
                                    Notification(
                                        type = "friend_request",
                                        fromUserId = fromUserId,
                                        fromUsername = user.username,
                                        profilePicture = user.profilePicture ?: ""
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("Notification", "Error loading user data", e)
                        }
                    }

                    // Observe message notifications
                    userDatabase.database.getReference("notifications")
                        .child(currentUserId)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                snapshot.children.forEach { notificationSnapshot ->
                                    val notification = notificationSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                                    notification?.let {
                                        if (it["type"] == "new_message") {
                                            viewModelScope.launch {
                                                val userSnapshot = userDatabase.getUserById(it["fromUserId"] as String)
                                                val profilePicture = userSnapshot.getValue<UserDatabaseModel>()?.profilePicture ?: ""
                                                notificationsList.add(
                                                    Notification(
                                                        type = "new_message",
                                                        fromUserId = it["fromUserId"] as String,
                                                        fromUsername = it["fromUsername"] as String,
                                                        timestamp = it["timestamp"] as Long,
                                                        profilePicture = profilePicture
                                                    )
                                                )
                                                _notifications.value = notificationsList.sortedByDescending { it.timestamp }
                                            }
                                        }
                                    }
                                }
                                _notifications.value = notificationsList.sortedByDescending { it.timestamp }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("Notification", "Error loading message notifications", error.toException())
                            }
                        })
                }
            },
            onError = { error ->
                Log.e("Notification", "Error loading notifications", error.toException())
            }
        )
    }

    fun markAsRead(notificationId: String) {
        val currentList = _notifications.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(isRead = true)
            _notifications.value = currentList
        }
    }

    fun clearNotification(notificationId: String) {
        _notifications.value = _notifications.value.filter { it.id != notificationId }
    }
}