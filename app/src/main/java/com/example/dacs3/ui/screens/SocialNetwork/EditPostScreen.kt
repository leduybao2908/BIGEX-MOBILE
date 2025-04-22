package com.example.dacs3.ui.screens.SocialNetwork

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.dacs3.ui.screens.SocialNetwork.model.Post
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    navController: NavController,
    postId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val realtimeDb = Firebase.database
    var originalPost by remember { mutableStateOf<Post?>(null) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    // Load post từ Firebase
    LaunchedEffect(postId) {
        val snapshot = realtimeDb.getReference("posts").child(postId).get().await()
        val post = snapshot.getValue(Post::class.java)
        post?.let {
            originalPost = it
            caption = it.caption
        }
    }

    // Decode ảnh base64 thành Bitmap nếu không chọn ảnh mới
    val displayBitmap = remember(originalPost?.imageBase64) {
        originalPost?.imageBase64?.takeIf { selectedImageUri == null }?.let {
            val bytes = Base64.decode(it, Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa bài viết") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                imagePickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            }) {
                Text("Chọn ảnh mới")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hiển thị ảnh mới hoặc ảnh gốc
            selectedImageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            } ?: displayBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Chú thích") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (caption.isNotBlank()) {
                        isUpdating = true

                        var imageBase64: String? = originalPost?.imageBase64

                        selectedImageUri?.let { uri ->
                            val bitmap: Bitmap = if (Build.VERSION.SDK_INT < 28) {
                                android.provider.MediaStore.Images.Media.getBitmap(
                                    context.contentResolver, uri
                                )
                            } else {
                                val source = ImageDecoder.createSource(context.contentResolver, uri)
                                ImageDecoder.decodeBitmap(source)
                            }

                            val outputStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            val byteArray = outputStream.toByteArray()
                            imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
                        }

                        val updatedPost = mapOf<String, Any?>(
                            "caption" to caption,
                            "imageBase64" to imageBase64
                        )

                        realtimeDb.getReference("posts").child(postId)
                            .updateChildren(updatedPost)
                            .addOnSuccessListener {
                                isUpdating = false
                                navController.popBackStack()
                            }
                            .addOnFailureListener {
                                isUpdating = false
                                errorMessage = "Lỗi khi cập nhật bài viết: ${it.localizedMessage}"
                            }
                    } else {
                        errorMessage = "Chú thích không được để trống"
                    }
                },
                enabled = !isUpdating
            ) {
                Text(if (isUpdating) "Đang cập nhật..." else "Cập nhật bài viết")
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
