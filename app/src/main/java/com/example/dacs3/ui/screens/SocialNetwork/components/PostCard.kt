package com.example.dacs3.ui.screens.SocialNetwork.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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

@Composable
fun PostCard(
    post: Post,
    onEditClick: (Post) -> Unit = {},
    onDeleteClick: (Post) -> Unit = {},
    onImageClick: (String) -> Unit = {} // Callback khi bấm vào ảnh
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
            // Header với avatar và tên người dùng
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sử dụng UserAvatar để hiển thị ảnh đại diện
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

            Spacer(modifier = Modifier.height(12.dp))

            // Ảnh bài đăng
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
                                post.imageBase64?.let { base64 ->
                                    onImageClick(base64)
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Caption
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
