package com.example.bodylens.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress_entries")
data class ProgressEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val weight: Float? = null, // in kg
    val notes: String = "",
    val aiAnalysisStatus: AnalysisStatus = AnalysisStatus.PENDING
)

enum class AnalysisStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    ERROR
}


