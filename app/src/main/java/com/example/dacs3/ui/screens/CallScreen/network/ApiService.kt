package com.example.dacs3.ui.screens.CallScreen.network

import retrofit2.http.Body
import retrofit2.http.POST

data class CreateOrderRequest(val amount: Int)

data class CreateOrderResponse(
    val success: Boolean,
    val paymentUrl: String?,
    val order: Map<String, Any>?
)

interface ApiService {
    @POST("/api/order")
    suspend fun createOrder(@Body body: Map<String, Any>): CreateOrderResponse
}
