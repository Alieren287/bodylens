package com.example.bodylens.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurements",
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
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long,
    val type: MeasurementType,
    val value: Float, // in cm or kg
    val timestamp: Long = System.currentTimeMillis()
)

enum class MeasurementType(val displayName: String, val unit: String) {
    CHEST("Chest", "cm"),
    WAIST("Waist", "cm"),
    HIPS("Hips", "cm"),
    ARMS("Arms", "cm"),
    THIGHS("Thighs", "cm"),
    WEIGHT("Weight", "kg"),
    BODY_FAT("Body Fat", "%")
}


