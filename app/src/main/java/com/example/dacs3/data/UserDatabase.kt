package com.example.dacs3.data

import android.util.Log
import com.example.dacs3.viewmodels.UserDatabaseModel
import com.example.dacs3.viewmodels.Transaction
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

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

    suspend fun createOrUpdateUser(user: UserDatabaseModel) {
        try {
            val existingUserSnapshot = usersRef.child(user.uid).get().await()
            if (existingUserSnapshot.exists()) {
                usersRef.child(user.uid).updateChildren(user.toMap()).await()
            } else {
                usersRef.child(user.uid).setValue(user).await()
            }
        } catch (e: Exception) {
            throw Exception("Failed to create or update user: ${e.message}")
        }
    }

    suspend fun checkEmailExists(email: String): Boolean {
        try {
            val snapshot = usersRef.orderByChild("email").equalTo(email).get().await()
            return snapshot.exists() && snapshot.childrenCount > 0
        } catch (e: Exception) {
            Log.e("UserDatabase", "Error checking email existence", e)
            return false
        }
    }

    suspend fun updateFcmToken(uid: String, fcmToken: String) {
        try {
            usersRef.child(uid).child("fcmToken").setValue(fcmToken).await()
        } catch (e: Exception) {
            throw Exception("Failed to update FCM token: ${e.message}")
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
                val offlineUpdates = hashMapOf<String, Any>(
                    "isOnline" to false,
                    "lastOnline" to ServerValue.TIMESTAMP
                )
                userRef.onDisconnect().updateChildren(offlineUpdates)
            } else {
                userRef.onDisconnect().cancel()
            }
            userRef.updateChildren(updates).await()
        } catch (e: Exception) {
            throw Exception("Failed to update user status: ${e.message}")
        }
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any>) {
        try {
            usersRef.child(uid).updateChildren(updates).await()
        } catch (e: Exception) {
            throw Exception("Failed to update user: ${e.message}")
        }
    }

    suspend fun addPointsAndTransaction(uid: String, points: Int, transaction: Transaction) {
        try {
            val userRef = usersRef.child(uid)
            val snapshot = userRef.get().await()
            val currentUser = snapshot.getValue(UserDatabaseModel::class.java)
            val newPoints = (currentUser?.points ?: 0) + points
            val newHistory = (currentUser?.transactionHistory ?: emptyList()) + transaction

            val updates = mapOf(
                "points" to newPoints,
                "transactionHistory" to newHistory
            )
            userRef.updateChildren(updates).await()
        } catch (e: Exception) {
            throw Exception("Failed to update points and transaction: ${e.message}")
        }
    }

    fun observeUsers(
        onDataChange: (List<UserDatabaseModel>) -> Unit,
        onError: (DatabaseError) -> Unit
    ) {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usersList = mutableListOf<UserDatabaseModel>()
                for (userSnapshot in snapshot.children) {
                    try {
                        if (!userSnapshot.hasChildren()) {
                            Log.e("observeUsers", "Invalid user at ${userSnapshot.key}: ${userSnapshot.value}")
                            continue
                        }
                        val user = userSnapshot.getValue(UserDatabaseModel::class.java)
                        if (user != null) {
                            usersList.add(user)
                        }
                    } catch (e: Exception) {
                        Log.e("observeUsers", "Error parsing user at ${userSnapshot.key}", e)
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
                "status" to "pending",
                "type" to "received",
                "timestamp" to ServerValue.TIMESTAMP
            )
            friendRequestsRef.child(toUserId).child(fromUserId).setValue(requestData).await()
        } catch (e: Exception) {
            throw Exception("Failed to send friend request: ${e.message}")
        }
    }

    suspend fun acceptFriendRequest(currentUserId: String, fromUserId: String) {
        try {
            friendRequestsRef.child(currentUserId).child(fromUserId).removeValue().await()
            friendRequestsRef.child(fromUserId).child(currentUserId).removeValue().await()
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
            friendRequestsRef.child(fromUserId).child(currentUserId).removeValue().await()
        } catch (e: Exception) {
            throw Exception("Failed to reject friend request: ${e.message}")
        }
    }

    fun observeFriendRequests(
        userId: String,
        onDataChange: (Map<String, Any>) -> Unit,
        onError: () -> Unit
    ) {
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
                onError()
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

    private fun UserDatabaseModel.toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "username" to username,
            "fullName" to fullName,
            "profilePicture" to profilePicture,
            "createdAt" to createdAt,
            "isOnline" to isOnline,
            "lastOnline" to lastOnline,
            "fcmToken" to fcmToken,
            "points" to points,
            "transactionHistory" to transactionHistory
        )
    }
}