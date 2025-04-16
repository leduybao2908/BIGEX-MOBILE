package com.example.dacs3.data

import com.google.firebase.database.IgnoreExtraProperties
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@IgnoreExtraProperties
data class TreeData(
    var treeState: TreeState = TreeState.Seed,
    var lastWateredDate: String = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE), // Store as String
    var userId: String = "",
    var treeId: String = ""
) {
    // No-argument constructor (required by Firebase)
    constructor() : this(TreeState.Seed, LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE), "", "")

    // Helper function to get LocalDate
    fun getLocalDate(): LocalDate {
        return LocalDate.parse(lastWateredDate, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    // Helper function to set LocalDate
    fun setLocalDate(date: LocalDate) {
        lastWateredDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}