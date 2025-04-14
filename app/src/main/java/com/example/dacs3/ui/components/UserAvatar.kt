package com.example.dacs3.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.*
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
    modifier: Modifier = Modifier,
    isOnline: Boolean? = null
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
                    val base64Data = when {
                        profilePicture.startsWith("data:image") -> profilePicture.substringAfter(",")
                        profilePicture.contains(",") -> profilePicture.substringAfter(",")
                        else -> profilePicture
                    }.trim()

                    if (base64Data.isNotEmpty()) {
                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    }
                }
            } catch (e: Exception) {
                error = true
                bitmap = null
            } finally {
                isLoading = false
            }
        } else {
            bitmap = null
            error = false
            isLoading = false
        }
    }

    Box(contentAlignment = Alignment.BottomEnd) {
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

        if (isOnline != null) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(size * 0.25f)
                    .clip(CircleShape)
                    .background(if (isOnline) androidx.compose.ui.graphics.Color.Green else androidx.compose.ui.graphics.Color.Red)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.surface,
                        CircleShape
                    )
            )
        }
    }
}
