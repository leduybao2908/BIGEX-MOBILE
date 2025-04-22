package com.example.dacs3.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import java.io.ByteArrayOutputStream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.ui.components.*
import com.example.dacs3.viewmodels.*
import androidx.compose.animation.*
import android.widget.Toast
import androidx.compose.ui.window.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    friendId: String,
    friendUsername: String,
    onNavigateBack: () -> Unit,
    onNavigateToVoiceCall: (String, String) -> Unit = { _, _ -> },
    viewModel: ChatViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {

        val messages by viewModel.messages.collectAsState()
        var messageText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        LaunchedEffect(messages) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(0)
            }
        }

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
                                size = 40.dp,
                                isOnline = viewModel.getFriendOnlineStatus(friendId)
                                    .also { isOnline ->
                                        android.util.Log.d(
                                            "MessageScreen",
                                            "Friend $friendId online status: $isOnline"
                                        )
                                    }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = friendUsername,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                val isOnline = viewModel.getFriendOnlineStatus(friendId)
                                val lastOnline = viewModel.getFriendLastOnline(friendId)
                                Text(
                                    text = if (isOnline) {
                                        "Đang hoạt động"
                                    } else {
                                        val timeDiff = System.currentTimeMillis() - lastOnline
                                        when {
                                            timeDiff < 60000 -> "Hoạt động vài giây trước"
                                            timeDiff < 3600000 -> "Hoạt động ${timeDiff / 60000} phút trước"
                                            timeDiff < 86400000 -> "Hoạt động ${timeDiff / 3600000} giờ trước"
                                            else -> "Hoạt động ${timeDiff / 86400000} ngày trước"
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOnline)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                onNavigateToVoiceCall(friendId, friendUsername)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Voice Call",
                                tint = MaterialTheme.colorScheme.primary
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
                // Messages list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    reverseLayout = true,
                    state = listState
                ) {
                    items(
                        items = messages.filter { msg ->
                            (msg.senderId == friendId) || (msg.receiverId == friendId)
                        }.reversed(),
                        key = { message -> message.id }
                    ) { message ->
                        val isCurrentUser = message.senderId == viewModel.currentUserId
                        MessageItem(message, isCurrentUser)
                    }

                }

                // Input field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    val imagePicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let {
                            val inputStream = context.contentResolver.openInputStream(it)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            val outputStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                            val imageBytes = outputStream.toByteArray()
                            val base64String = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                            viewModel.sendMessage(friendId, base64String, isImage = true)
                        }
                    }

                    IconButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Chọn ảnh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun MessageItem(
        message: Message,
        isCurrentUser: Boolean,
        viewModel: ChatViewModel = viewModel()
    ) {
        var showReactionMenu by remember { mutableStateOf(false) }
        var showImageDialog by remember { mutableStateOf(false) }
        val reactions = listOf("❤️", "👍", "😊", "😢", "😡", "👏")
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
            ) {
                Column(
                    horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
                ) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrentUser)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .widthIn(max = 340.dp)
                                .combinedClickable(
                                    onClick = { if (message.isImage) showImageDialog = true },
                                    onLongClick = { showReactionMenu = true }
                                )
                        ) {
                            Box {
                                if (message.isImage) {
                                    val imageBytes = remember(message.content) {
                                        Base64.decode(message.content, Base64.DEFAULT)
                                    }
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = imageBytes,
                                            onError = {
                                                Log.e(
                                                    "MessageItem",
                                                    "Error loading image",
                                                    it.result.throwable
                                                )
                                            }
                                        ),
                                        contentDescription = "Hình ảnh",
                                        modifier = Modifier
                                            .size(200.dp)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = message.content,
                                        modifier = Modifier.padding(12.dp),
                                        color = if (isCurrentUser)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Reactions display to the right of the message with animation
                AnimatedVisibility(
                    visible = message.reactions.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        message.reactions.forEach { (userId, emoji) ->
                            Text(
                                text = emoji,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .clickable {
                                        if (userId == viewModel.currentUserId) {
                                            viewModel.removeReaction(message.id)
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            // Image Dialog
            if (showImageDialog && message.isImage) {
                Dialog(
                    onDismissRequest = { showImageDialog = false }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.8f)
                            .clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val imageBytes = remember(message.content) {
                                Base64.decode(message.content, Base64.DEFAULT)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(imageBytes),
                                    contentDescription = "Hình ảnh chi tiết",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Button(
                                onClick = {
                                    val filename = "IMG_${System.currentTimeMillis()}.jpg"
                                    val contentValues = android.content.ContentValues().apply {
                                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                                    }

                                    val resolver = context.contentResolver
                                    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                                    try {
                                        uri?.let {
                                            resolver.openOutputStream(it)?.use { outputStream ->
                                                outputStream.write(imageBytes)
                                            }
                                            Toast.makeText(context, "Đã lưu ảnh", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Lỗi khi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text("Tải xuống")
                            }
                        }
                    }
                }
            }

            // Reaction menu popup
            if (showReactionMenu) {
                AnimatedVisibility(
                    visible = showReactionMenu,
                    enter = fadeIn() + scaleIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + scaleOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reactions.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            viewModel.addReaction(message.id, emoji)
                                            showReactionMenu = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun MessageItemPreview() {
        val textMessage = Message(
            id = "1",
            senderId = "user1",
            receiverId = "user2",
            content = "Hello, this is a sample message that can be long enough to test multiline",
            timestamp = System.currentTimeMillis(),
            isImage = false,
            reactions = mapOf(
                "user1" to "❤️"
            )
        )

        val imageMessage = Message(
            id = "2",
            senderId = "user2",
            receiverId = "user1",
            content = "/9j/4AAQSkZJRg...", // Base64 image sample
            timestamp = System.currentTimeMillis(),
            isImage = true,
            reactions = mapOf(
                "user3" to "😊"
            )
        )

        Column {
            MessageItem(
                message = textMessage,
                isCurrentUser = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            MessageItem(
                message = textMessage,
                isCurrentUser = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            MessageItem(
                message = imageMessage,
                isCurrentUser = false
            )
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



