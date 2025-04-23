package com.example.dacs3

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dacs3.data.UserPreferences
import com.example.dacs3.navigation.*
import com.example.dacs3.ui.components.BottomBar
import com.example.dacs3.ui.components.BottomBarScreen
import com.example.dacs3.ui.screens.auth.LoginScreen
import com.example.dacs3.ui.screens.auth.SignUpScreen
import com.example.dacs3.ui.screens.points.PointsScreen
import com.example.dacs3.ui.screens.tree.*
import com.example.dacs3.ui.theme.DACS3Theme
import com.example.dacs3.viewmodels.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        CoroutineScope(Dispatchers.Main).launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration
            )
            if (result == SnackbarResult.ActionPerformed) {
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
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun getTestFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showSnackbar("FCM Token retrieved. Check Logcat for the token.")
                } else {
                    showSnackbar("Failed to get FCM token")
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()
        getTestFCMToken()

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
                                onLogin = { email, password ->
                                    authViewModel.login(email, password)
                                },
                                authViewModel = authViewModel
                            )
                        }

                        composable(Screen.SignUp.route) {
                            SignUpScreen(
                                navController = navController,
                                onSignUp = { username, email, password ->
                                    authViewModel.signUp(username, email, password)
                                },
                                authViewModel = authViewModel
                            )
                        }

                        composable(BottomBarScreen.tree.route) {
                            TreeScreen(
                                onNavigateToPoints = {
                                    navController.navigate("points")
                                }
                            )
                        }

                        composable("points") {
                            PointsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    LaunchedEffect(authState) {
                        when (authState) {
                            is AuthState.Success -> {
                                navController.navigate(BottomBarScreen.tree.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                            is AuthState.Error -> {
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
                            }
                            null -> {}
                        }
                    }
                }
            }
        }
    }
}