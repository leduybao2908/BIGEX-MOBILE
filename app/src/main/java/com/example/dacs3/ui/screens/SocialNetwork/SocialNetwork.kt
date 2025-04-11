package com.example.dacs3.ui.screens.SocialNetwork

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dacs3.ui.screens.SocialNetwork.components.PostCard
import com.example.dacs3.ui.screens.SocialNetwork.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

@Composable
fun SocialNetwork(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }

    LaunchedEffect(true) {
        firestore.collection("posts")
            .get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { doc ->
                    try {
                        Post(
                            id = doc.id,
                            caption = doc.getString("caption") ?: "",
                            imageBase64 = doc.getString("imageBase64"),
                            timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                posts = list
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
                    PostCard(post = posts[index])
                }
            }
        }
    }
}
