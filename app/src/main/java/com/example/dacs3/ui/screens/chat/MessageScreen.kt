package com.example.dacs3.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.ui.components.*
import com.example.dacs3.viewmodels.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    friendId: String,
    friendUsername: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        notificationViewModel.notifications.value
            .filter { it.fromUserId == friendId && !it.isRead }
            .forEach { notification ->
                notificationViewModel.markAsRead(notification.id)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            username = friendUsername,
                            profilePicture = viewModel.getFriendProfilePicture(friendId),
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = friendUsername)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.filter { msg ->
                    (msg.senderId == friendId) || (msg.receiverId == friendId)
                }.reversed()) { message ->
                    val isCurrentUser = message.senderId == viewModel.currentUserId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 340.dp)
                        ) {
                            Text(
                                text = message.content,
                                modifier = Modifier.padding(12.dp),
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text("Nhập tin nhắn...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(friendId, messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi tin nhắn",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
@Preview
@Composable
fun MessageScreenPreview() {
    MessageScreen(
        friendId = "friendId",
        friendUsername = "Friend Username",
        onNavigateBack = {}
    )
}