package com.example.dacs3.ui.screens.SocialNetwork

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
//import androidx.room.util.copy
import com.example.dacs3.ui.screens.SocialNetwork.components.PostCard
import com.example.dacs3.ui.screens.SocialNetwork.model.Post
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
//import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import com.example.dacs3.data.User

@Composable
fun SocialNetwork(navController: NavController) {
    val realtimeDb = Firebase.database
    val posts = remember { mutableStateListOf<Post>() }
    val context = LocalContext.current

    // Sử dụng LaunchedEffect để đảm bảo gọi trong Coroutine Scope
    LaunchedEffect(Unit) {
        // Sử dụng coroutineScope để làm việc với dữ liệu trong background
        coroutineScope {
            val postsRef = realtimeDb.getReference("posts")
            postsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Post>()
                    for (postSnap in snapshot.children) {
                        val post = postSnap.getValue(Post::class.java)
                        post?.let { list.add(it.copy(id = postSnap.key ?: "")) }
                    }
                    // Cập nhật lại trong UI thread
                    posts.clear()
                    posts.addAll(list.sortedByDescending { it.timestamp })
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Lỗi khi load bài viết: ${error.message}")
                }
            })
        }
    }

    fun deletePost(postId: String, onComplete: () -> Unit) {
        realtimeDb.getReference("posts").child(postId).removeValue()
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { e ->
                println("Lỗi khi xóa bài: ${e.localizedMessage}")
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
                        onEditClick = { post ->
                            navController.navigate("edit_post/${post.id}")
                        },
                        onDeleteClick = { post ->
                            deletePost(post.id) {
                                // Realtime sẽ tự cập nhật lại danh sách
                            }
                        }
                    )
                }
            }
        }
    }
}


