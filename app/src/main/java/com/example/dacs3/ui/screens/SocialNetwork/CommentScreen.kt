package com.example.dacs3.ui.screens.SocialNetwork

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dacs3.ui.screens.SocialNetwork.model.Comment
import com.google.firebase.database.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    postId: String,
    navController: androidx.navigation.NavController // Nếu cần quay lại
) {
    // Danh sách comment lưu trong state để UI cập nhật khi thêm mới
    val commentList = remember { mutableStateListOf<Comment>() }
    var newComment by remember { mutableStateOf("") }

    // Khởi tạo reference Firebase Realtime Database
    val databaseReference = FirebaseDatabase.getInstance().getReference("posts").child(postId).child("comments")

    // Lấy danh sách comment từ Firebase khi màn hình được tạo
    LaunchedEffect(postId) {
        getCommentsFromFirebase(postId, commentList)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }

        // Danh sách bình luận
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(commentList) { comment ->
                CommentItem(comment)
            }
        }

        // Gửi bình luận mới
        TextField(
            value = newComment,
            onValueChange = { newComment = it },
            label = { Text("Write a comment...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                addComment(
                    content = newComment,
//                    userId = "demo_user", // TODO: lấy từ Auth
//                    userName = "Demo User", // TODO: lấy từ Auth
                    postId = postId
                )
                newComment = ""
            },
            enabled = newComment.isNotBlank()
        ) {
            Text("Post Comment")
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = comment.userName, fontWeight = FontWeight.Bold)
        Text(text = comment.content)
    }
}

fun addComment(
    content: String,
    postId: String
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        Log.e("Comment", "User not logged in")
        return
    }

    val userId = currentUser.uid

    // Lấy userName từ "users/{uid}/username"
    val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
    userRef.get().addOnSuccessListener { snapshot ->
        val userName = snapshot.child("username").getValue(String::class.java) ?: "Unknown"

        val newComment = Comment(
            userId = userId,
            userName = userName,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        val databaseReference = FirebaseDatabase.getInstance()
            .getReference("posts")
            .child(postId)
            .child("comments")

        val newCommentId = databaseReference.push().key
        newCommentId?.let {
            databaseReference.child(it).setValue(newComment).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Comment", "Comment added successfully!")
                } else {
                    Log.e("Comment", "Failed to add comment", task.exception)
                }
            }
        }
    }.addOnFailureListener {
        Log.e("Comment", "Failed to fetch username", it)
    }
}

fun getCommentsFromFirebase(postId: String, commentList: MutableList<Comment>) {
    val databaseReference = FirebaseDatabase.getInstance().getReference("posts").child(postId).child("comments")

    databaseReference.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            commentList.clear()
            for (data in snapshot.children) {
                // Kiểm tra xem có dữ liệu bình luận và parse đúng kiểu
                val comment = data.getValue(Comment::class.java)
                if (comment != null) {
                    commentList.add(comment)
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Error getting comments: ${error.message}")
        }
    })
}

