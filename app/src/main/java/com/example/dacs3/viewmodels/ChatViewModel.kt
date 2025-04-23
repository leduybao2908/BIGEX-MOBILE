package com.example.dacs3.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.UserDatabase
import com.example.dacs3.data.UserDatabaseModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.dacs3.service.NotificationService
import kotlinx.coroutines.*
import java.util.*

// ... (Message data class remains unchanged)

data class Message(
    @get:PropertyName("id")
    @PropertyName("id")
    val id: String = "",

    @get:PropertyName("senderId")
    @PropertyName("senderId")
    val senderId: String = "",

    @get:PropertyName("receiverId")
    @PropertyName("receiverId")
    val receiverId: String = "",

    @get:PropertyName("content")
    @PropertyName("content")
    val content: String = "",

    @get:PropertyName("timestamp")
    @PropertyName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @get:PropertyName("senderName")
    @PropertyName("senderName")
    val senderName: String = "",

    @get:PropertyName("senderProfilePicture")
    @PropertyName("senderProfilePicture")
    val senderProfilePicture: String = "",

    @get:PropertyName("isRead")
    @PropertyName("isRead")
    var isRead: Boolean = false,

    @get:PropertyName("isImage")
    @PropertyName("isImage")
    val isImage: Boolean = false,

    @get:PropertyName("reactions")
    @PropertyName("reactions")
    val reactions: Map<String, String> = mapOf()
)

class ChatViewModel(private val context: Context) : ViewModel() {
    private var onlineStatusJob: Job? = null
    private val database = FirebaseDatabase.getInstance("https://dacs3-5cf79-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val messagesRef = database.getReference("messages")
    private val userDatabase = UserDatabase()
    private val auth = FirebaseAuth.getInstance()
    private val notificationService = NotificationService(context)

    val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _friends = MutableStateFlow<List<UserDatabaseModel>>(emptyList())
    val friends: StateFlow<List<UserDatabaseModel>> = _friends

    init {
        onlineStatusJob = viewModelScope.launch {
            updateUserOnlineStatus()
            loadFriends()
            observeMessages()
        }
    }

    private fun loadFriends() {
        val currentUserId = auth.currentUser?.uid ?: return
        val friendsRef = database.getReference("friends").child(currentUserId)

        friendsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { friendSnapshot ->
                    val friendId = friendSnapshot.key ?: return@forEach

                    // Create a separate listener for each friend's online status
                    val userRef = database.getReference("users").child(friendId)
                    userRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val isOnline = userSnapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                            val lastOnline = userSnapshot.child("lastOnline").getValue(Long::class.java) ?: 0L

                            // Log the status update
                            Log.d("ChatViewModel", "Friend $friendId status update - isOnline: $isOnline, lastOnline: $lastOnline")

                            // Update friend data in the list
                            val friend = userSnapshot.getValue(UserDatabaseModel::class.java)?.copy(
                                uid = friendId,
                                isOnline = isOnline
                            )

                            if (friend != null) {
                                val currentList = _friends.value.toMutableList()
                                val index = currentList.indexOfFirst { it.uid == friendId }

                                if (index != -1) {
                                    currentList[index] = friend
                                } else {
                                    currentList.add(friend)
                                }

                                _friends.value = currentList
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("ChatViewModel", "Error monitoring friend status", error.toException())
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatViewModel", "Error loading friends", error.toException())
            }
        })
    }

    private fun observeMessages() {
        val currentUserId = auth.currentUser?.uid ?: return

        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messagesList = mutableListOf<Message>()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null && (message.senderId == currentUserId || message.receiverId == currentUserId)) {
                        // Tự động đánh dấu tin nhắn là đã đọc nếu người dùng hiện tại là người gửi
                        if (message.senderId == currentUserId) {
                            message.isRead = true
                        }
                        messagesList.add(message)
                        // Tạo thông báo cho tin nhắn mới nếu người nhận là người dùng hiện tại
                        if (message.receiverId == currentUserId && message.timestamp > System.currentTimeMillis() - 5000) {
                            database.getReference("notifications")
                                .child(currentUserId)
                                .push()
                                .setValue(mapOf(
                                    "type" to "new_message",
                                    "fromUserId" to message.senderId,
                                    "fromUsername" to message.senderName,
                                    "content" to message.content,
                                    "timestamp" to message.timestamp,
                                    "isRead" to false
                                ))
                        }
                    }
                }
                _messages.value = messagesList.sortedBy { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun getFriendProfilePicture(friendId: String): String {
        return friends.value.find { it.uid == friendId }?.profilePicture ?: ""
    }

    fun getLastMessage(friendId: String): String? {
        return messages.value
            .filter { msg ->
                (msg.senderId == friendId && msg.receiverId == currentUserId) ||
                        (msg.senderId == currentUserId && msg.receiverId == friendId)
            }
            .maxByOrNull { it.timestamp }
            ?.let { msg ->
                if (msg.isImage) "Hình ảnh" else msg.content
            }
    }

    fun getUnreadCount(friendId: String): Int {
        return messages.value.filter { msg ->
            msg.senderId == friendId &&
                    msg.receiverId == currentUserId &&
                    !msg.isRead
        }.distinctBy { it.id }.size
    }

    fun addReaction(messageId: String, emoji: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageRef = messagesRef.child(messageId)
        messageRef.child("reactions").child(currentUserId).setValue(emoji)
    }

    fun removeReaction(messageId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageRef = messagesRef.child(messageId)
        messageRef.child("reactions").child(currentUserId).removeValue()
    }

    fun getLastMessageTime(friendId: String): String? {
        val lastMessage = messages.value
            .filter { msg ->
                (msg.senderId == friendId && msg.receiverId == currentUserId) ||
                        (msg.senderId == currentUserId && msg.receiverId == friendId)
            }
            .maxByOrNull { it.timestamp }
            ?: return null

        val now = System.currentTimeMillis()
        val diff = now - lastMessage.timestamp
        val hours = diff / (60 * 60 * 1000)
        val days = hours / 24

        return when {
            hours < 24 -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = lastMessage.timestamp }
                String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
            }
            days == 1L -> "Hôm qua"
            else -> {
                val calendar = Calendar.getInstance().apply { timeInMillis = lastMessage.timestamp }
                String.format("%02d/%02d/%d",
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.YEAR)
                )
            }
        }
    }

    fun getFriendOnlineStatus(friendId: String): Boolean {
        val friend = friends.value.find { it.uid == friendId }
        return friend?.isOnline == true
    }

    fun cleanup() {
        onlineStatusJob?.cancel()
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            database.getReference("users")
                .child(currentUserId)
                .child("isOnline")
                .setValue(false)
            database.getReference("users")
                .child(currentUserId)
                .child("lastOnline")
                .setValue(System.currentTimeMillis())
        }
    }

    private fun updateUserOnlineStatus() {
        val currentUserId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(currentUserId)

        // Set online status
        userRef.child("isOnline").setValue(true)
        userRef.child("lastOnline").setValue(ServerValue.TIMESTAMP)

        // Set up disconnect hook
        userRef.child("isOnline").onDisconnect().setValue(false)
        userRef.child("lastOnline").onDisconnect().setValue(ServerValue.TIMESTAMP)
    }

    private suspend fun getUserById(userId: String): UserDatabaseModel? {
        return try {
            val snapshot = userDatabase.getUserById(userId)
            snapshot.getValue(UserDatabaseModel::class.java)?.copy(uid = userId)
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error getting user by ID", e)
            null
        }
    }

    fun sendMessage(receiverId: String, content: String, isImage: Boolean = false) {
        val currentUser = auth.currentUser ?: return
        val messageId = messagesRef.push().key ?: return

        viewModelScope.launch {
            try {
                // Get current user's data from database
                val currentUserSnapshot = userDatabase.getUserById(currentUser.uid)
                val currentUserData = currentUserSnapshot.getValue(UserDatabaseModel::class.java)

                // Get receiver's data
                val receiverData = getUserById(receiverId)

                messagesRef.child(messageId).setValue(mapOf(
                    "id" to messageId,
                    "senderId" to currentUser.uid,
                    "receiverId" to receiverId,
                    "content" to content,
                    "timestamp" to System.currentTimeMillis(),
                    "senderName" to (currentUserData?.username ?: ""),
                    "senderProfilePicture" to (currentUserData?.profilePicture ?: ""),
                    "isRead" to false,
                    "isImage" to isImage
                ))

                // Send push notification to receiver only if we have their token
                receiverData?.fcmToken?.let { token ->
                    if (token.isNotEmpty()) {
                        notificationService.sendMessageNotification(
                            token = token,
                            currentUserData?.username ?: "",
                            if (isImage) "Đã gửi một hình ảnh" else content,
                            senderId = currentUser.uid,
                            senderName = currentUserData?.username ?: "",
                            messageId = messageId
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
            }
        }
    }

    fun markMessageAsRead(messageId: String) {
        val message = messages.value.find { it.id == messageId } ?: return
        messagesRef.child(messageId).child("isRead").setValue(true)
    }

    fun getFriendLastOnline(friendId: String): Long {
        val friend = friends.value.find { it.uid == friendId }
        return friend?.lastOnline ?: System.currentTimeMillis()
    }
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
