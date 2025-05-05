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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPostScreen(
    navController: NavController,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val realtimeDb = Firebase.database
    val currentUser = Firebase.auth.currentUser

    // Trạng thái để lưu Uri ảnh đã chọn
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Trạng thái cho chú thích (caption)
    var caption by remember { mutableStateOf("") }

    // Trạng thái cho biết đang upload bài viết
    var isUploading by remember { mutableStateOf(false) }

    // Trạng thái thông báo lỗi (nếu có)
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Khởi tạo launcher để chọn ảnh từ thư viện
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }

    // Scaffold chứa TopAppBar và nội dung màn hình
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đăng bài viết") },
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
            // Nút chọn ảnh
            Button(onClick = {
                imagePickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            }) {
                Text("Chọn ảnh từ thư viện")
            }

            // Hiển thị ảnh đã chọn
            selectedImageUri?.let { uri ->
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nhập chú thích bài viết
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Chú thích") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nút đăng bài viết
            Button(
                onClick = {
                    if (caption.isNotBlank() && currentUser != null) {
                        isUploading = true

                        var imageBase64: String? = null

                        // Nếu có ảnh được chọn, chuyển ảnh thành chuỗi Base64 để lưu lên Firebase
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

                        // Lấy thông tin người dùng từ Firebase để lưu vào bài viết
                        val userRef = realtimeDb.getReference("users").child(currentUser.uid)
                        userRef.get().addOnSuccessListener { snapshot ->
                            val userName = snapshot.child("username").getValue(String::class.java) ?: "Không tên"
                            val userAvatar = snapshot.child("profilePicture").getValue(String::class.java)

                            // Tạo ID mới cho bài viết
                            val postId = realtimeDb.reference.child("posts").push().key
                            if (postId != null) {
                                // Tạo object bài viết
                                val post = mapOf(
                                    "caption" to caption,
                                    "userId" to currentUser.uid,
                                    "userName" to userName,
                                    "userAvatar" to userAvatar,
                                    "timestamp" to System.currentTimeMillis(),
                                    "imageBase64" to imageBase64
                                )

                                // Lưu bài viết lên Firebase
                                realtimeDb.reference.child("posts").child(postId)
                                    .setValue(post)
                                    .addOnSuccessListener {
                                        isUploading = false
                                        caption = ""
                                        selectedImageUri = null
                                        navController.popBackStack() // Quay lại màn trước
                                    }
                                    .addOnFailureListener { e ->
                                        isUploading = false
                                        errorMessage = "Lỗi khi lưu trạng thái: ${e.localizedMessage}"
                                    }
                            }
                        }.addOnFailureListener { e ->
                            isUploading = false
                            errorMessage = "Không lấy được thông tin người dùng: ${e.localizedMessage}"
                        }
                    } else {
                        errorMessage = "Chú thích không được để trống"
                    }
                },
                enabled = !isUploading
            ) {
                Text(if (isUploading) "Đang đăng..." else "Đăng bài")
            }

            // Hiển thị lỗi nếu có
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
