package com.example.dacs3.ui.screens.SocialNetwork

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dacs3.ui.screens.SocialNetwork.components.PostCard
import com.example.dacs3.ui.screens.SocialNetwork.ImageHolder
import com.example.dacs3.ui.screens.SocialNetwork.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@Composable
fun SocialNetwork(navController: NavController) {
    val realtimeDb = Firebase.database
    val posts = remember { mutableStateListOf<Post>() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    LaunchedEffect(Unit) {
        val postsRef = realtimeDb.getReference("posts").orderByChild("timestamp")
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Post>()
                for (postSnap in snapshot.children) {
                    val post = postSnap.getValue(Post::class.java)
                    post?.let {
                        list.add(it.copy(id = postSnap.key ?: ""))
                    }
                }
                posts.clear()
                posts.addAll(list.reversed())  // Đảm bảo bài viết mới nhất ở đầu
            }

            override fun onCancelled(error: DatabaseError) {
                println("Lỗi khi load bài viết: ${error.message}")
            }
        })
    }

    // Hàm xử lý reaction
// Hàm xử lý reaction
    fun handleReactionClick(postId: String, type: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid
        val reactionRef = Firebase.database.getReference("posts").child(postId).child("reactions").child(uid)

        reactionRef.get().addOnSuccessListener { snapshot ->
            val existingType = snapshot.getValue(String::class.java)

            // Kiểm tra lại nếu "type" đã có và không đổi thì xóa reaction
            if (existingType == type) {
                reactionRef.removeValue()  // Nếu reaction đã có, xoá nó
            } else {
                reactionRef.setValue(type)  // Cập nhật reaction mới
            }
        }.addOnFailureListener { exception ->
            Log.e("handleReactionClick", "Error handling reaction: ${exception.message}")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("upload_post") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Đăng bài")
            }
        }
    ) { paddingValues ->
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Chưa có bài đăng nào")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(posts.size) { index ->
                    PostCard(
                        post = posts[index],
                        currentUserId = currentUserId, // 👈 truyền uid người dùng hiện tại
                        navController = navController, // ✅ THÊM DÒNG NÀY
                        onEditClick = { post ->
                            navController.navigate("edit_post/${post.id}")
                        },
                        onDeleteClick = { post ->
                            realtimeDb.getReference("posts").child(post.id).removeValue()
                        },
                        onImageClick = { imageBase64 ->
                            ImageHolder.base64Image = imageBase64
                            navController.navigate("full_image")
                        },
                        onReactionClick = { postId, type ->
                            handleReactionClick(postId, type)
                        },
                        onCommentClick = { postId ->
                            navController.navigate("comments/$postId")
                        }
                    )
                }
            }
        }
    }
}
