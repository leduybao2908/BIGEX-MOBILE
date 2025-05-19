package com.example.dacs3.data

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class PointsData(
    var userId: String = "",
    var points: Int = 0,
    var redemptionHistory: MutableList<RedemptionRecord> = mutableListOf()
) {
    constructor() : this("", 0, mutableListOf())

    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "points" to points,
            "redemptionHistory" to redemptionHistory
        )
    }
}

data class RedemptionRecord(
    var type: String = "", // "voucher" or "charity"
    var amount: Int = 0, // Points spent
    var details: String = "", // e.g., "$5 Voucher" or "Charity Donation"
    var timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", 0, "", System.currentTimeMillis())

    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "type" to type,
            "amount" to amount,
            "details" to details,
            "timestamp" to timestamp
        )
    }
}