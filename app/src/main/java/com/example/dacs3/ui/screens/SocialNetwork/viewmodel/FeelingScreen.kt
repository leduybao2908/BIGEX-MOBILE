package com.example.dacs3.ui.screens.SocialNetwork.viewmodel

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dacs3.ui.components.UserAvatar
import com.example.dacs3.ui.screens.SocialNetwork.components.formatTimestamp
import com.example.dacs3.ui.screens.SocialNetwork.model.ReactionInfo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun FeelingScreen(postId: String, navController: NavController) {
    val reactions = remember { mutableStateListOf<ReactionInfo>() }

    LaunchedEffect(postId) {
        val reactionRef = FirebaseDatabase.getInstance()
            .getReference("posts").child(postId).child("reactions")

        reactionRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reactions.clear()
                snapshot.children.forEach { userSnap ->
                    val userId = userSnap.key ?: return@forEach
                    val type = userSnap.getValue(String::class.java) ?: return@forEach

                    FirebaseDatabase.getInstance().getReference("users").child(userId)
                        .get().addOnSuccessListener { userSnap ->
                            val userName = userSnap.child("username").getValue(String::class.java) ?: "Unknown"
                            val avatar = userSnap.child("avatar").getValue(String::class.java) ?: ""

                            reactions.add(
                                ReactionInfo(
                                    userId = userId,
                                    userName = userName,
                                    avatar = avatar,
                                    type = type,  // Sử dụng type là String
                                    timestamp = 0L  // Mặc định timestamp là 0
                                )
                            )
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FeelingScreen", "Error: ${error.message}")
            }
        })
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Danh sách cảm xúc", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(reactions) { reaction ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    UserAvatar(reaction.userName, reaction.avatar, size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("${reaction.userName} đã thả ${reaction.type}")
                        Text(formatTimestamp(reaction.timestamp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

