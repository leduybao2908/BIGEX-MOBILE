package com.example.dacs3.ui.screens.tree

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.R
import com.example.dacs3.data.TreeState
import com.example.dacs3.viewmodels.TreeViewModel
import com.example.dacs3.viewmodels.TreeViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun TreeScreen(
    onNavigateToPoints: () -> Unit
) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val treeViewModel: TreeViewModel = viewModel(factory = TreeViewModelFactory(context, userId))
    val activeTree by treeViewModel.activeTree.observeAsState()
    val isWateredToday by treeViewModel.isWateredToday.observeAsState(false)
    var fcmToken by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }

    // Request notification permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tree Image
        val treeImage = when (activeTree?.treeState) {
            TreeState.Seed -> R.drawable.seed
            TreeState.Sprout -> R.drawable.sprout
            TreeState.Sapling -> R.drawable.sapling
            TreeState.Tree -> R.drawable.tree
            null -> R.drawable.ground
        }

        Image(
            painter = painterResource(id = treeImage),
            contentDescription = "Tree",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Watering Status
        Text(
            text = if (isWateredToday) "Hôm nay đã tưới cây!" else "Hôm nay chưa tưới cây.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Water Button
        if (!isWateredToday) {
            Button(onClick = { treeViewModel.waterTree(fcmToken) }) {
                Text("Tưới cây")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Watering History
        Text("Lịch sử tưới cây:", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 100.dp)
                .fillMaxWidth()
        ) {
            items(activeTree?.wateringHistory ?: emptyList()) { date ->
                Text(date, modifier = Modifier.padding(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reminder Setting
        Button(onClick = { showTimePicker = true }) {
            Text("Đặt lịch nhắc nhở")
        }
        Text(
            text = activeTree?.let {
                if (it.reminderHour != null && it.reminderMinute != null) {
                    "Nhắc nhở lúc: ${String.format("%02d:%02d", it.reminderHour, it.reminderMinute)}"
                } else {
                    "Chưa đặt lịch nhắc nhở"
                }
            } ?: "Chưa đặt lịch nhắc nhở",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Navigate to Points Screen
        Button(onClick = onNavigateToPoints) {
            Text("Xem Điểm Thưởng")
        }

        if (showTimePicker) {
            TimePickerDialog(
                onDismiss = { showTimePicker = false },
                onConfirm = { hour, minute ->
                    treeViewModel.setWateringReminder(hour, minute)
                    showTimePicker = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}