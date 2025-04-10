package com.example.dacs3.ui.screens.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dacs3.ui.viewmodels.*
import com.example.dacs3.data.UserInterest
import com.example.dacs3.data.VietnamCity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import android.app.DatePickerDialog
import androidx.compose.material.icons.automirrored.filled.*
import java.text.SimpleDateFormat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBase64 by remember { mutableStateOf<String?>(currentUser?.profilePicture) }
    var username by remember { mutableStateOf(currentUser?.username ?: "") }
    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var birthdate by remember { mutableStateOf(currentUser?.birthdate ?: "") }
    var phone by remember { mutableStateOf(currentUser?.phone ?: "") }
    var selectedInterests by remember { mutableStateOf<List<UserInterest>>(currentUser?.interests ?: emptyList()) }
    var selectedCity by remember { mutableStateOf<VietnamCity?>(currentUser?.city) }
    var showInterestsMenu by remember { mutableStateOf(false) }
    var showCityMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    // Format for displaying the date
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            birthdate = dateFormatter.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            selectedImageUri = it
            // Convert Uri to Base64
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                inputStream?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                    val imageBytes = baos.toByteArray()
                    selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to process image: ${e.message}"
                showErrorDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cập nhật thông tin") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val context = LocalContext.current
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                imageLoader = coil.ImageLoader(context)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Add Profile Picture",
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Picture",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(4.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue -> name = newValue },
                    label = { Text("Họ và tên") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = username,
                    onValueChange = { newValue -> username = newValue },
                    label = { Text("Tên đăng nhập") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = birthdate,
                    onValueChange = { },
                    label = { Text("Ngày sinh") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    },
                    readOnly = true,
                    enabled = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            datePickerDialog.show()
                        }) {
                            Icon(Icons.Default.DateRange, "Chọn ngày")
                        }
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue -> phone = newValue },
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    }
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedInterests.joinToString(", ") { it.toString() },
                        onValueChange = { },
                        label = { Text("Sở thích") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Favorite, contentDescription = null)
                        },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showInterestsMenu = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Chọn sở thích")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showInterestsMenu,
                        onDismissRequest = { showInterestsMenu = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        UserInterest.values().forEach { interest ->
                            DropdownMenuItem(
                                text = { Text(interest.toString()) },
                                onClick = {
                                    selectedInterests = if (selectedInterests.contains(interest)) {
                                        selectedInterests - interest
                                    } else {
                                        selectedInterests + interest
                                    }
                                },
                                trailingIcon = {
                                    if (selectedInterests.contains(interest)) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCity?.toString() ?: "",
                        onValueChange = { },
                        label = { Text("Thành phố") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.LocationCity, contentDescription = null)
                        },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showCityMenu = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Chọn thành phố")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showCityMenu,
                        onDismissRequest = { showCityMenu = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        VietnamCity.values().forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city.toString()) },
                                onClick = {
                                    selectedCity = city
                                    showCityMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        isLoading = true
                        val updatedFields = mutableMapOf<String, String>()
                        if (username != currentUser?.username) updatedFields["username"] = username
                        if (name != currentUser?.name) updatedFields["name"] = name
                        if (birthdate != currentUser?.birthdate) updatedFields["birthdate"] = birthdate
                        if (phone != currentUser?.phone) updatedFields["phone"] = phone
                        if (selectedInterests != currentUser?.interests) updatedFields["interests"] = selectedInterests.toString()
                        if (selectedCity != currentUser?.city) updatedFields["city"] = selectedCity.toString()
                        if (selectedImageBase64 != currentUser?.profilePicture) updatedFields["profilePictureBase64"] = selectedImageBase64 ?: ""
                        
                        authViewModel.updateUserProfile(
                            username = updatedFields["username"] ?: currentUser?.username ?: "",
                            birthdate = updatedFields["birthdate"] ?: currentUser?.birthdate ?: "",
                            phone = updatedFields["phone"] ?: currentUser?.phone ?: "",
                            interests = (updatedFields["interests"] ?: currentUser?.interests ?: "").toString(),
                            city = (updatedFields["city"] ?: currentUser?.city ?: "").toString(),
                            profilePictureBase64 = updatedFields["profilePictureBase64"] ?: currentUser?.profilePicture ?: ""
                        )
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Cập nhật")
                }
            }
        }
    }
}
