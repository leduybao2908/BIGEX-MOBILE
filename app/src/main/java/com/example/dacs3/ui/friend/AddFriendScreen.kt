package com.example.dacs3.ui.screens.friend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.viewmodels.AddFriendViewModel
import com.example.dacs3.viewmodels.UserDatabaseModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddFriendViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kết Bạn") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Tìm kiếm người dùng") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                val filteredUsers = users.filter {
                    it.username.contains(searchQuery, ignoreCase = true) ||
                            it.email.contains(searchQuery, ignoreCase = true)
                }
                items(filteredUsers) { user ->
                    UserItem(
                        user = user,
                        friendRequests = friendRequests,
                        snackbarHostState = snackbarHostState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserItem(
    user: UserDatabaseModel,
    friendRequests: Map<String, Any>,
    snackbarHostState: SnackbarHostState,
    viewModel: AddFriendViewModel
) {
    val request = friendRequests[user.uid] as? Map<*, *>
    val requestStatus = request?.get("status") as? String
    val isReceivedRequest = request?.get("type") == "received"
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                requestStatus == "pending" && isReceivedRequest -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            when {
                requestStatus == "pending" && !isReceivedRequest -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Đã gửi")
                    }
                }
                requestStatus == "pending" && isReceivedRequest -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.acceptFriendRequest(user.uid)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Đã chấp nhận kết bạn",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Chấp nhận")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.rejectFriendRequest(user.uid)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Đã từ chối kết bạn",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        ) {
                            Text("Từ chối")
                        }
                    }
                }
                requestStatus == null -> {
                    Button(
                        onClick = {
                            isLoading = true
                            viewModel.sendFriendRequest(user.uid)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Đã gửi yêu cầu kết bạn",
                                    duration = SnackbarDuration.Short
                                )
                                isLoading = false
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Kết bạn")
                        }
                    }
                }
            }
        }
    }
}