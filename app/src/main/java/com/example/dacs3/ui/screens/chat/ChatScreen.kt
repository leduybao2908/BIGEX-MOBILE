package com.example.dacs3.ui.screens.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.ui.components.*
import com.example.dacs3.viewmodels.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    onNavigateToAddFriend: () -> Unit,
    onNavigateToMessage: (String, String) -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val friends by viewModel.friends.collectAsState()
    var searchQuery by remember { mutableStateOf("") }



    LaunchedEffect(friends) {
        friends.forEach { friend ->
            if (viewModel.getUnreadCount(friend.uid) == 0) {
                viewModel.messages.value
                    .filter { msg ->
                        msg.senderId == friend.uid &&
                                msg.receiverId == viewModel.currentUserId &&
                                !msg.isRead
                    }
                    .forEach { msg ->
                        viewModel.markMessageAsRead(msg.id)
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                actions = {
                    IconButton(onClick = onNavigateToAddFriend) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Friend"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Tìm kiếm") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                )
            )

            LazyColumn(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(friends.filter {
                    it.username.contains(searchQuery, ignoreCase = true)
                }) { friend ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {  viewModel.messages.value
                            .filter { msg ->
                                msg.senderId == friend.uid &&
                                        msg.receiverId == viewModel.currentUserId &&
                                        !msg.isRead
                            }
                            .forEach { msg ->
                                viewModel.markMessageAsRead(msg.id)
                            }
                            onNavigateToMessage(friend.uid, friend.username)  }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    UserAvatar(
                                        username = friend.username,
                                        profilePicture = friend.profilePicture,
                                        size = 56.dp,
                                        isOnline = friend.isOnline
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = friend.username,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = viewModel.getLastMessage(friend.uid)?.let { message ->
                                            val lastMessage = viewModel.messages.value.find { it.content == message }
                                            if (lastMessage?.senderId == viewModel.currentUserId) {
                                                if (lastMessage?.isImage == true) "Bạn: Hình ảnh" else "Bạn: $message"
                                            } else {
                                                if (lastMessage?.isImage == true) "Hình ảnh" else message
                                            }
                                        } ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (viewModel.getUnreadCount(friend.uid) > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = viewModel.getUnreadCount(friend.uid)
                                                    .toString(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = viewModel.getLastMessageTime(friend.uid) ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { /* TODO: Implement video call */ },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VideoCall,
                                            contentDescription = "Video Call",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { /* TODO: Implement camera */ },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "Camera",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


}

@Preview
@Composable
fun ChatScreenPreview() {
    ChatScreen(
        onNavigateToAddFriend = {},
        onNavigateToMessage = { _, _ -> }
    )
}

