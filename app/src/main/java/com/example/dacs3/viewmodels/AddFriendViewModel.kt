package com.example.dacs3.viewmodels

import android.util.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.UserDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddFriendViewModel : ViewModel() {
    private val userDatabase = UserDatabase()
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://dacs3-5cf79-default-rtdb.asia-southeast1.firebasedatabase.app")

    private val _users = MutableStateFlow<List<UserDatabaseModel>>(emptyList())
    val users: StateFlow<List<UserDatabaseModel>> = _users

    private val _friendRequests = MutableStateFlow<Map<String, Any>>(emptyMap())
    val friendRequests: StateFlow<Map<String, Any>> = _friendRequests

    init {
        loadUsers()
        loadFriendRequests()
    }

    private fun loadUsers() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) return

        // Observe friends list first
        database.getReference("friends")
            .child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(friendsSnapshot: DataSnapshot) {
                    val friendIds = friendsSnapshot.children.mapNotNull { it.key }.toSet()

                    // Then observe all users and filter out friends
                    userDatabase.observeUsers(
                        onDataChange = { allUsers ->
                            _users.value = allUsers.filter { user ->
                                user.uid != currentUserId && !friendIds.contains(user.uid)
                            }
                        },
                        onError = { error ->
                            Log.e("AddFriend", "Error loading users", error.toException())
                        }
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AddFriend", "Error loading friends", error.toException())
                }
            })
    }

    private fun loadFriendRequests() {
        val currentUserId = auth.currentUser?.uid ?: return
        userDatabase.observeFriendRequests(
            currentUserId,
            onDataChange = { requests ->
                _friendRequests.value = requests
            },
            onError = {
                // Handle error
            }
        )
    }

    fun sendFriendRequest(receiverId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                Log.d("FriendRequest", "Sending friend request from $currentUserId to $receiverId")

                // Add friend request to receiver's node only
                database.getReference("friend_requests")
                    .child(receiverId)
                    .child(currentUserId)
                    .setValue(mapOf(
                        "status" to "pending",
                        "type" to "received",
                        "timestamp" to ServerValue.TIMESTAMP
                    ))

                // Update local state to show pending status
                val currentRequests = _friendRequests.value.toMutableMap()
                currentRequests[receiverId] = mapOf(
                    "status" to "pending",
                    "type" to "sent",
                    "timestamp" to System.currentTimeMillis()
                )
                _friendRequests.value = currentRequests

                // Add notification for receiver
                database.getReference("notifications")
                    .child(receiverId)
                    .push()
                    .setValue(mapOf(
                        "type" to "friend_request",
                        "fromUserId" to currentUserId,
                        "timestamp" to ServerValue.TIMESTAMP,
                        "isRead" to false
                    ))

                Log.i("FriendRequest", "Friend request sent successfully to $receiverId")
            } catch (e: Exception) {
                Log.e("FriendRequest", "Error sending friend request", e)
            }
        }
    }

    fun acceptFriendRequest(fromUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                // Add users as friends
                database.getReference("friends")
                    .child(currentUserId)
                    .child(fromUserId)
                    .setValue(true)

                database.getReference("friends")
                    .child(fromUserId)
                    .child(currentUserId)
                    .setValue(true)

                // Remove friend requests
                database.getReference("friend_requests")
                    .child(currentUserId)
                    .child(fromUserId)
                    .removeValue()

                database.getReference("friend_requests")
                    .child(fromUserId)
                    .child(currentUserId)
                    .removeValue()
            } catch (e: Exception) {
                Log.e("FriendRequest", "Error accepting friend request", e)
            }
        }
    }

    fun rejectFriendRequest(fromUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                // Remove friend requests
                database.getReference("friend_requests")
                    .child(currentUserId)
                    .child(fromUserId)
                    .removeValue()

                database.getReference("friend_requests")
                    .child(fromUserId)
                    .child(currentUserId)
                    .removeValue()
            } catch (e: Exception) {
                Log.e("FriendRequest", "Error rejecting friend request", e)
            }
        }
    }
}