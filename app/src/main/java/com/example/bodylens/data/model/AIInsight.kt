package com.example.bodylens.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_insights",
    foreignKeys = [
        ForeignKey(
            entity = ProgressEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId")]
)
data class AIInsight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long,
    val insightType: InsightType,
    val content: String,
    val confidence: Float = 0f, // 0-1 confidence score
    val timestamp: Long = System.currentTimeMillis()
)

enum class InsightType {
    POSTURE_ANALYSIS,
    MUSCLE_DEVELOPMENT,
    BODY_COMPOSITION,
    PROGRESS_TREND,
    RECOMMENDATIONS
}


