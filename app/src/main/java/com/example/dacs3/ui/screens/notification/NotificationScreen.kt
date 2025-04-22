package com.example.dacs3.ui.screens.notification

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.ui.components.*
import com.example.dacs3.viewmodels.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.ZoneId

fun Long.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    notificationViewModel: NotificationViewModel = viewModel(),
    addFriendViewModel: AddFriendViewModel = viewModel(),
    onNavigateToMessage: (String, String) -> Unit = { _, _ -> },
    chatViewModel: ChatViewModel = viewModel() // Giữ lại ChatViewModel để sử dụng
) {
    val notifications by notificationViewModel.notifications.collectAsState()

    // Nhóm thông báo theo thời gian
    val groupedNotifications = notifications.groupBy { notification ->
        val now = LocalDateTime.now()
        val notificationTime = notification.timestamp.toLocalDateTime()
        when {
            ChronoUnit.DAYS.between(notificationTime, now) == 0L -> "Hôm nay"
            ChronoUnit.DAYS.between(notificationTime, now) == 1L -> "Hôm qua"
            ChronoUnit.WEEKS.between(notificationTime, now) == 0L -> "Tuần này"
            else -> "Cũ hơn"
        }
    }.toSortedMap(compareBy {
        when (it) {
            "Hôm nay" -> 0
            "Hôm qua" -> 1
            "Tuần này" -> 2
            else -> 3
        }
    })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông báo") }
            )
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Không có thông báo nào")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                groupedNotifications.forEach { (timeGroup, notifications) ->
                    item {
                        Text(
                            text = timeGroup,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    val groupedByUser = notifications.groupBy { it.fromUserId }
                    items(groupedByUser.toList()) { (userId, userNotifications) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (userNotifications.any { !it.isRead })
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        // Navigate to chat screen when notification is clicked
                                        if (userNotifications.isNotEmpty()) {
                                            onNavigateToMessage(userId, userNotifications.first().fromUsername)
                                            // Mark notifications as read
                                            userNotifications.forEach { notification ->
                                                if (!notification.isRead) {
                                                    notificationViewModel.markAsRead(notification.id)
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    // Avatar
                                    UserAvatar(
                                        username = userNotifications.firstOrNull()?.fromUsername ?: "",
                                        profilePicture = userNotifications.firstOrNull()?.profilePicture ?: ""
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = userNotifications.firstOrNull()?.fromUsername ?: "N/A",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = userNotifications.firstOrNull()?.timestamp?.toLocalDateTime()?.format(
                                                DateTimeFormatter.ofPattern("HH:mm")
                                            ) ?: "Unknown",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                when (userNotifications.firstOrNull()?.type) {
                                    "friend_request" -> {
                                        Text(
                                            text = "Đã gửi lời mời kết bạn",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    addFriendViewModel.rejectFriendRequest(userId)
                                                    userNotifications.forEach { notification ->
                                                        notificationViewModel.clearNotification(notification.id)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Từ chối"
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Từ chối")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    addFriendViewModel.acceptFriendRequest(userId)
                                                    userNotifications.forEach { notification ->
                                                        notificationViewModel.clearNotification(notification.id)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Chấp nhận"
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Chấp nhận")
                                            }
                                        }
                                    }

                                    "new_message" -> {
                                        val unreadCount = chatViewModel.getUnreadCount(userId)
                                        if (unreadCount > 0) {
                                            Button(
                                                onClick = {
                                                    // Mark messages as read when clicking button
                                                    chatViewModel.messages.value
                                                        .filter { msg -> msg.senderId == userId && msg.receiverId == chatViewModel.currentUserId && !msg.isRead }
                                                        .forEach { msg -> chatViewModel.markMessageAsRead(msg.id) }
                                                    onNavigateToMessage(userId, userNotifications.firstOrNull()?.fromUsername ?: "")
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.surface
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .padding(8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "$unreadCount tin nhắn mới",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                                    )
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier
                                                    .padding(8.dp).fillMaxWidth().clickable {
                                                        chatViewModel.messages.value
                                                            .filter { msg -> msg.senderId == userId && msg.receiverId == chatViewModel.currentUserId && !msg.isRead }
                                                            .forEach { msg -> chatViewModel.markMessageAsRead(msg.id) }
                                                        onNavigateToMessage(userId, userNotifications.firstOrNull()?.fromUsername ?: "")
                                                    },
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Tin nhắn mới",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.align(Alignment.CenterHorizontally)
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
    }
}

