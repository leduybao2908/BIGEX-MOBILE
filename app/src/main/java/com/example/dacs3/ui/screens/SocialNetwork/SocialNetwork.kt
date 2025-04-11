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
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SocialNetwork(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var posts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Tải bài viết từ Firestore
    LaunchedEffect(true) {
        firestore.collection("posts")
            .get()
            .addOnSuccessListener { result ->
                val list = result.map { it.data }
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
