package com.example.dacs3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.*
import androidx.core.content.ContextCompat
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dacs3.ui.screens.SocialNetwork.CommentScreen
import com.example.dacs3.data.UserPreferences
import com.example.dacs3.navigation.*
import com.example.dacs3.ui.components.*
import com.example.dacs3.ui.screens.auth.LoginScreen
import com.example.dacs3.ui.screens.auth.SignUpScreen
import com.example.dacs3.ui.screens.chat.*
import com.example.dacs3.ui.screens.notification.*
import com.example.dacs3.ui.screens.profile.*
import com.example.dacs3.ui.theme.DACS3Theme
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.dacs3.ui.screens.SocialNetwork.SocialNetwork
import com.example.dacs3.ui.screens.SocialNetwork.UploadPostScreen
import com.example.dacs3.ui.screens.SocialNetwork.FullImageScreen
import com.example.dacs3.ui.screens.SocialNetwork.EditPostScreen
import com.example.dacs3.ui.screens.tree.*

import com.example.dacs3.viewmodels.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.app.AlertDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.ui.screens.SocialNetwork.viewmodel.FeelingScreen
import com.example.dacs3.services.VideoCallService
import com.example.dacs3.ui.screens.CallScreen.CallScreen
import com.example.dacs3.ui.screens.CallScreen.PaymentScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.dacs3.ui.screens.chat.VideoCallScreen

class MainActivity : ComponentActivity() {
    private val snackbarHostState = SnackbarHostState()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showSnackbar("Notification permission granted")
        } else {
            showSnackbar(
                message = "Notification permission denied. You can enable it in Settings.",
                actionLabel = "Settings",
                duration = SnackbarDuration.Long
            ) {
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
        }
    }

    private fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        action: (() -> Unit)? = null
    ) {
        lifecycleScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration
            ).also {
                action?.invoke()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Permission Required")
                    .setMessage("We need 'Draw over other apps' permission to enable offline call feature.")
                    .setPositiveButton("Grant") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        }
    }

    private fun getTestFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM", "Test FCM Token: $token")
                    showSnackbar("FCM Token retrieved. Check Logcat for the token.")
                } else {
                    Log.e("FCM", "Failed to get FCM token", task.exception)
                    showSnackbar("Failed to get FCM token")
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()
        getTestFCMToken()
        checkAndRequestOverlayPermission()

        // Start VideoCallService
        val serviceIntent = Intent(this, VideoCallService::class.java).apply {
            putExtra("userId", FirebaseAuth.getInstance().currentUser?.uid)
        }
        startService(serviceIntent)

        val userPreferences = UserPreferences(this)
        val authViewModel = AuthViewModel(userPreferences)

        setContent {
            DACS3Theme {
                val navController = rememberNavController()
                val authState by authViewModel.authState.collectAsState()
                val authEvent by authViewModel.authEvent.collectAsState()

                Scaffold(
                    bottomBar = {
                        if (authState is AuthState.Success) {
                            BottomBar(navController = navController)
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Login.route,
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(
                                navController = navController,
                                onLogin = { email, password -> authViewModel.login(email, password) },
                                authViewModel = authViewModel
                            )
                        }

                        composable(Screen.SignUp.route) {
                            SignUpScreen(
                                navController = navController,
                                onSignUp = { username, email, password -> authViewModel.signUp(username, email, password) },
                                authViewModel = authViewModel
                            )
                        }

                        composable(BottomBarScreen.Profile.route) {
                            ProfileScreen(
                                authViewModel = authViewModel,
                                onNavigateToUpdateProfile = { navController.navigate("update_profile") }
                            )
                        }

                        composable("update_profile") {
                            UpdateProfileScreen(
                                onNavigateBack = { navController.popBackStack() },
                                authViewModel = authViewModel
                            )
                        }

                        composable(BottomBarScreen.Chat.route) {
                            ChatScreen(
                                onNavigateToAddFriend = { navController.navigate(Screen.AddFriend.route) },
                                onNavigateToMessage = { uid, username -> navController.navigate("message/$uid/$username") },
                                onNavigateToVideoCall = { callerId, callerName ->
                                    navController.navigate("video_call/$callerId/$callerName")
                                }
                            )
                        }

                        composable(Screen.AddFriend.route) {
                            AddFriendScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("comments/{postId}") { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString("postId") ?: ""
                            CommentScreen(postId = postId, navController = navController)
                        }

                        composable("feelings/{postId}") { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                            FeelingScreen(postId = postId, navController = navController)
                        }

                        composable(
                            route = "video_call/{callerId}/{callerName}",
                            arguments = listOf(
                                navArgument("callerId") { type = NavType.StringType },
                                navArgument("callerName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val callerId = backStackEntry.arguments?.getString("callerId") ?: return@composable
                            val callerName = backStackEntry.arguments?.getString("callerName") ?: return@composable

                            // Sử dụng viewModel() để tạo ViewModel đúng cách
                            val videoCallViewModel: VideoCallViewModel = viewModel()

                            VideoCallScreen(
                                viewModel = videoCallViewModel
                            )
                        }



                        composable("full_image") {
                            FullImageScreen()
                        }


                        composable(BottomBarScreen.Social.route) {
                            SocialNetwork(navController = navController)
                        }

                        composable(BottomBarScreen.Call.route) {
                            CallScreen(navController = navController)
                        }
                        composable("payment") {
                            PaymentScreen(navController)
                        }

                        composable(BottomBarScreen.tree.route) {
                            TreeScreen()
                        }

                        composable(BottomBarScreen.Notification.route) {
                            NotificationScreen(
                                onNavigateToMessage = { userId, username -> navController.navigate("message/$userId/$username") }
                            )
                        }

                        composable(
                            route = "edit_post/{postId}",
                            arguments = listOf(navArgument("postId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString("postId") ?: ""
                            EditPostScreen(
                                navController = navController,
                                postId = postId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("upload_post") {
                            UploadPostScreen(
                                onNavigateBack = { navController.popBackStack() },
                                navController = navController
                            )
                        }

                        composable(
                            route = "message/{uid}/{username}",
                            deepLinks = listOf(navDeepLink { uriPattern = "android-app://androidx.navigation/message/{uid}/{username}" })
                        ) { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            val username = backStackEntry.arguments?.getString("username") ?: return@composable
                            MessageScreen(
                                friendId = uid,
                                friendUsername = username,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToVideoCall = { callerId, callerName ->
                                    navController.navigate("video_call/$callerId/$callerName")
                                }
                            )
                        }
                    }

                    LaunchedEffect(authState) {
                        when (authState) {
                            is AuthState.Success -> {
                                navController.navigate(BottomBarScreen.Profile.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                            is AuthState.Error -> {
                                // Error will be handled in the respective screens
                            }
                            else -> {}
                        }
                    }

                    LaunchedEffect(authEvent) {
                        when (authEvent) {
                            is AuthEvent.NavigateToLogin -> {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                                authViewModel.clearEvents()
                            }
                            is AuthEvent.ShowError -> {
                                // Error will be handled in the respective screens
                            }
                            null -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DACS3Theme {
        Greeting("Android")
    }
}