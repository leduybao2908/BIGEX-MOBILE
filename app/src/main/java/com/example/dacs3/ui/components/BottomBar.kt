package com.example.dacs3.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dacs3.R

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val iconResId: Int
) {
    object Chat : BottomBarScreen(
        route = "chat",
        title = "Chat",
        iconResId = R.drawable.ic_chat
    )

    object Social : BottomBarScreen(
        route = "social",
        title = "Social",
        iconResId = R.drawable.ic_social
    )

    object Call : BottomBarScreen(
        route = "call",
        title = "Call",
        iconResId = R.drawable.ic_social
    )

    object tree : BottomBarScreen(
        route = "tree",
        title = "tree",
        iconResId = R.drawable.ic_tree
    )

    object Notification : BottomBarScreen(
        route = "notification",
        title = "Noti",
        iconResId = R.drawable.ic_notification
    )

    object Profile : BottomBarScreen(
        route = "profile",
        title = "Profile",
        iconResId = R.drawable.ic_profile
    )
}

@Composable
fun BottomBar(navController: NavController) {
    val screens = listOf(
        BottomBarScreen.Chat,
        BottomBarScreen.Social,
        BottomBarScreen.tree,
        BottomBarScreen.Call,
        BottomBarScreen.Notification,
        BottomBarScreen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        screens.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = screen.iconResId),
                        contentDescription = screen.title,
                        tint = if (selected)
                                MaterialTheme.colorScheme.primary
                              else
                                MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = { Text(text = screen.title) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}