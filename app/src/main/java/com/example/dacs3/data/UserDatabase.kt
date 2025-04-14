package com.example.dacs3.data

import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserDatabaseModel(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val fullName: String = "",
    val profilePicture: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val lastOnline: Long = System.currentTimeMillis()
)

class UserDatabase {
    val database = FirebaseDatabase.getInstance("https://dacs3-5cf79-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val usersRef = database.getReference("users")
    private val friendsRef = database.getReference("friends")
    private val friendRequestsRef = database.getReference("friend_requests")

    suspend fun createUser(user: UserDatabaseModel) {
        try {
            usersRef.child(user.uid).setValue(user).await()
        } catch (e: Exception) {
            throw Exception("Failed to create user in database: ${e.message}")
        }
    }

    suspend fun updateUserStatus(uid: String, isOnline: Boolean) {
        try {
            val userRef = usersRef.child(uid)
            val updates = hashMapOf<String, Any>(
                "isOnline" to isOnline,
                "lastOnline" to if (!isOnline) System.currentTimeMillis() else ServerValue.TIMESTAMP
            )
            
            if (isOnline) {
                // Khi user online, thiết lập onDisconnect để tự động cập nhật trạng thái offline khi mất kết nối
                val offlineUpdates = hashMapOf<String, Any>(
                    "isOnline" to false,
                    "lastOnline" to ServerValue.TIMESTAMP
                )
                userRef.onDisconnect().updateChildren(offlineUpdates)
            } else {
                // Khi user offline có chủ ý, hủy onDisconnect handler
                userRef.onDisconnect().cancel()
            }
            
            userRef.updateChildren(updates).await()
        } catch (e: Exception) {
            throw Exception("Failed to update user status: ${e.message}")
        }
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any>) {
        try {
            database.reference.child("users").child(uid).updateChildren(updates).await()
        } catch (e: Exception) {
            throw Exception("Failed to update user: ${e.message}")
        }
    }

    fun observeUsers(onDataChange: (List<UserDatabaseModel>) -> Unit, onError: (DatabaseError) -> Unit) {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usersList = mutableListOf<UserDatabaseModel>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(UserDatabaseModel::class.java)
                    if (user != null) {
                        usersList.add(user)
                    }
                }
                onDataChange(usersList)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error)
            }
        })
    }

    suspend fun sendFriendRequest(fromUserId: String, toUserId: String) {
        try {
            val requestData = hashMapOf<String, Any>(
                "fromUserId" to fromUserId,
                "status" to "pending",
                "timestamp" to ServerValue.TIMESTAMP
            )
            friendRequestsRef.child(toUserId).child(fromUserId).setValue(requestData).await()
        } catch (e: Exception) {
            throw Exception("Failed to send friend request: ${e.message}")
        }
    }

    suspend fun acceptFriendRequest(currentUserId: String, fromUserId: String) {
        try {
            // Remove the friend request
            friendRequestsRef.child(currentUserId).child(fromUserId).removeValue().await()
            
            // Create bidirectional friend relationship
            val updates = hashMapOf<String, Any>(
                "$currentUserId/$fromUserId" to true,
                "$fromUserId/$currentUserId" to true
            )
            friendsRef.updateChildren(updates).await()
        } catch (e: Exception) {
            throw Exception("Failed to accept friend request: ${e.message}")
        }
    }

    suspend fun rejectFriendRequest(currentUserId: String, fromUserId: String) {
        try {
            friendRequestsRef.child(currentUserId).child(fromUserId).removeValue().await()
        } catch (e: Exception) {
            throw Exception("Failed to reject friend request: ${e.message}")
        }
    }

    fun observeFriendRequests(userId: String, onDataChange: (Map<String, Any>) -> Unit, onError: (DatabaseError) -> Unit) {
        friendRequestsRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = mutableMapOf<String, Any>()
                for (requestSnapshot in snapshot.children) {
                    val requestData = requestSnapshot.getValue() as? Map<String, Any>
                    if (requestData != null) {
                        requests[requestSnapshot.key!!] = requestData
                    }
                }
                onDataChange(requests)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error)
            }
        })
    }

    suspend fun getUserById(userId: String): DataSnapshot {
        return try {
            usersRef.child(userId).get().await()
        } catch (e: Exception) {
            throw Exception("Failed to get user data: ${e.message}")
        }
    }
}