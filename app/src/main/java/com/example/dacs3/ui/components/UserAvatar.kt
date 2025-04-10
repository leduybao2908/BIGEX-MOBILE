package com.example.dacs3.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.*
import coil.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun UserAvatar(
    username: String,
    profilePicture: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(profilePicture) {
        if (!profilePicture.isNullOrEmpty()) {
            isLoading = true
            error = false
            try {
                withContext(Dispatchers.IO) {
                    val base64Data = profilePicture.substringAfter(",")
                    val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                }
            } catch (e: Exception) {
                error = true
            } finally {
                isLoading = false
            }
        } else {
            bitmap = null
            error = false
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(size / 2),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Text(
                    text = username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
