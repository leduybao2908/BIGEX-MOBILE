package com.example.dacs3.ui.screens.CallScreen
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun PaymentScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun callPaymentApi() {
        isLoading = true
        errorMessage = null

        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://5870-2001-ee1-db04-d6d0-c9ba-c1f3-3c32-a610.ngrok-free.app/api/create-order")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                val jsonBody = JSONObject()
                jsonBody.put("amount", 10000)
                jsonBody.put("userId", "123")

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP error code: $responseCode")
                }

                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                println("Response text: $responseText")

                val json = JSONObject(responseText)
                val success = json.getBoolean("success")

                scope.launch(Dispatchers.Main) {
                    isLoading = false
                    if (success) {
                        val paymentUrl = json.getString("paymentUrl")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
                        context.startActivity(intent)
                    } else {
                        errorMessage = "Không tạo được URL thanh toán."
                    }
                }
            } catch (e: Exception) {
                scope.launch(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "Lỗi khi gọi API: ${e.message}"
                }
            }
        }
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(onClick = { callPaymentApi() }) {
                    Text("Thanh toán VNPay")
                }
            }
        }
    }
}
