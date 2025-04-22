package com.example.dacs3.data

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@IgnoreExtraProperties
data class TreeData(
    var treeState: TreeState = TreeState.Seed,
    var lastWateredDate: String = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
    var wateringHistory: MutableList<String> = mutableListOf(), // Store all watered dates
    var reminderHour: Int? = null, // Reminder hour
    var reminderMinute: Int? = null, // Reminder minute
    var userId: String = "",
    var treeId: String = ""
) {
    constructor() : this(
        TreeState.Seed,
        LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
        mutableListOf(),
        null,
        null,
        "",
        ""
    )

    @Exclude
    fun getLocalDate(): LocalDate {
        return LocalDate.parse(lastWateredDate, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @Exclude
    fun setLocalDate(date: LocalDate) {
        lastWateredDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}