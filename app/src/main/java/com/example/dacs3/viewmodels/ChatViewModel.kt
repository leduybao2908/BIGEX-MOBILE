package com.example.dacs3.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.UserDatabase
import com.example.dacs3.data.UserDatabaseModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.dacs3.service.NotificationService
import java.util.*

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
    var isRead: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance("https://dacs3-5cf79-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val messagesRef = database.getReference("messages")
    private val userDatabase = UserDatabase()
    private val auth = FirebaseAuth.getInstance()
    private val notificationService = NotificationService()

    // Expose current user ID
    val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _friends = MutableStateFlow<List<UserDatabaseModel>>(emptyList())
    val friends: StateFlow<List<UserDatabaseModel>> = _friends

    init {
        loadFriends()
        observeMessages()
        // Khởi tạo coroutine để ghi log tin nhắn chưa đọc mỗi giây
        viewModelScope.launch {
            while(true) {
                friends.value.forEach { friend ->
                    val unreadCount = getUnreadCount(friend.uid)
                    if (unreadCount > 0) {
                        println("[${friend.username}] có $unreadCount tin nhắn chưa đọc")
                    }
                }
                kotlinx.coroutines.delay(1000) // Đợi 1 giây
            }
        }
    }

    private fun loadFriends() {
        val currentUserId = auth.currentUser?.uid ?: return
        val friendsRef = database.getReference("friends").child(currentUserId)

        friendsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    val friendsList = mutableListOf<UserDatabaseModel>()
                    for (friendSnapshot in snapshot.children) {
                        val friendId = friendSnapshot.key ?: continue
                        // Fetch friend's user data
                        database.getReference("users").child(friendId)
                            .get()
                            .addOnSuccessListener { userSnapshot ->
                                val friend = userSnapshot.getValue(UserDatabaseModel::class.java)
                                if (friend != null) {
                                    friendsList.add(friend)
                                    _friends.value = friendsList
                                }
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
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
            ?.content
    }

    fun getUnreadCount(friendId: String): Int {
        return messages.value.filter { msg ->
            msg.senderId == friendId &&
            msg.receiverId == currentUserId &&
            !msg.isRead
        }.distinctBy { it.id }.size
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

    fun sendMessage(receiverId: String, content: String) {
        val currentUser = auth.currentUser ?: return
        val messageId = messagesRef.push().key ?: return

        viewModelScope.launch {
            try {
                // Get current user's data from database
                val currentUserSnapshot = userDatabase.getUserById(currentUser.uid)
                val currentUserData = currentUserSnapshot.getValue(UserDatabaseModel::class.java)

                messagesRef.child(messageId).setValue(mapOf(
                    "id" to messageId,
                    "senderId" to currentUser.uid,
                    "receiverId" to receiverId,
                    "content" to content,
                    "timestamp" to System.currentTimeMillis(),
                    "senderName" to (currentUserData?.username ?: ""),
                    "senderProfilePicture" to (currentUserData?.profilePicture ?: ""),
                    "isRead" to false
                ))

                // Tạo thông báo cho người nhận
                database.getReference("notifications")
                    .child(receiverId)
                    .push()
                    .setValue(mapOf(
                        "type" to "new_message",
                        "fromUserId" to currentUser.uid,
                        "fromUsername" to (currentUserData?.username ?: ""),
                        "content" to content,
                        "timestamp" to System.currentTimeMillis(),
                        "isRead" to false
                    ))
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun markMessageAsRead(messageId: String) {
        val message = messages.value.find { it.id == messageId } ?: return
        messagesRef.child(messageId).child("isRead").setValue(true)
    }
}

