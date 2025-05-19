package com.example.dacs3.ui.screens.CallScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.dacs3.ui.components.BottomBarScreen

@Composable
fun CallScreen(navController: NavController) {
    // Giả lập trạng thái người dùng VIP hay không
    val isVip = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (!isVip.value) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Bạn chưa phải là VIP")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    // Điều hướng đến màn hình thanh toán
                    navController.navigate("payment")
                }) {
                    Text("Mở khóa VIP ngay")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    navController.navigate("video_call")
                }) {
                    Text("Dùng thử miễn phí")
                }
            }
        } else {
            Text(text = "Đây là màn hình gọi điện")
        }
    }
}

fun NavGraphBuilder.callScreenRoute(navController: NavController) {
    composable(BottomBarScreen.Call.route) {
        CallScreen(navController)
    }
}
