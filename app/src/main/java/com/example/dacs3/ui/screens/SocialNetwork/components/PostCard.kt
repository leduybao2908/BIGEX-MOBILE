package com.example.dacs3.ui.screens.SocialNetwork.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dacs3.ui.components.UserAvatar
import com.example.dacs3.ui.screens.SocialNetwork.model.Post
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController

@Composable
fun PostCard(
    post: Post,
    navController: NavController,
    currentUserId: String, // 👈 thêm dòng này
    onEditClick: (Post) -> Unit = {},
    onDeleteClick: (Post) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onReactionClick: (postId: String, reactionType: String) -> Unit = { _, _ -> },
    onCommentClick: (postId: String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: User and options (edit, delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        username = post.userName,
                        profilePicture = post.userAvatar,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = post.userName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = formatTimestamp(post.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Box {
                    if (currentUserId == post.userId) {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Chỉnh sửa") },
                                onClick = {
                                    expanded = false
                                    onEditClick(post)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa") },
                                onClick = {
                                    expanded = false
                                    onDeleteClick(post)
                                }
                            )
                        }
                    }
                }

            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display image if available
            post.imageBase64?.let {
                val bitmap = remember(post.imageBase64) {
                    val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                post.imageBase64?.let { base64 -> onImageClick(base64) }
                            },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Display post caption
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reactions and comments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reactions Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Calculate the count of each reaction type
                    val reactionsCount = mutableMapOf<String, Int>()
                    post.reactions.forEach { (_, reactionType) ->
                        reactionsCount[reactionType] = reactionsCount.getOrDefault(reactionType, 0) + 1
                    }

                    // Calculate total reactions (you can also display reaction counts for each type)
                    val totalReactions = (reactionsCount["like"] ?: 0) +
                            (reactionsCount["love"] ?: 0) +
                            (reactionsCount["haha"] ?: 0)


                    // Display the total number of reactions
                    Text(
                        text = "❤️ $totalReactions",
                        modifier = Modifier.clickable {
                            navController.navigate("feelings/${post.id}") // ← Gọi màn hình danh sách cảm xúc
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Display the number of comments
                    Text(
                        text = "💬 ${post.comments.size}",
                        modifier = Modifier.clickable {
                            onCommentClick(post.id)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Reaction buttons: like, love, haha
                Row {
                    Text("👍", modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            onReactionClick(post.id, "like")  // Lưu cảm xúc là "like"
                        })
                    Text("❤️", modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            onReactionClick(post.id, "love")  // Lưu cảm xúc là "love"
                        })
                    Text("😂", modifier = Modifier
                        .clickable {
                            onReactionClick(post.id, "haha")  // Lưu cảm xúc là "haha"
                        })
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
