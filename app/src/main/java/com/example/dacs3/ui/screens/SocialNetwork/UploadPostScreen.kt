package com.example.dacs3.ui.screens.SocialNetwork

import android.net.Uri
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPostScreen(
    navController: NavController,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }

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
            Button(onClick = {
                imagePickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            }) {
                Text("Chọn ảnh từ thư viện")
            }

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

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Chú thích") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (caption.isNotBlank() && currentUser != null) {
                        isUploading = true
                        val post = hashMapOf(
                            "caption" to caption,
                            "userId" to currentUser.uid,
                            "timestamp" to Timestamp.now(),
                            "imageUrl" to (selectedImageUri?.toString() ?: "")
                        )

                        db.collection("posts")
                            .add(post)
                            .addOnSuccessListener {
                                isUploading = false
                                caption = ""
                                selectedImageUri = null
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                isUploading = false
                                errorMessage = "Lỗi khi lưu trạng thái: ${e.localizedMessage}"
                            }
                    } else {
                        errorMessage = "Chú thích không được để trống"
                    }
                },
                enabled = !isUploading
            ) {
                Text(if (isUploading) "Đang đăng..." else "Đăng bài")
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
