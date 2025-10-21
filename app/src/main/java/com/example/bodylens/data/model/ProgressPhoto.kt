package com.example.bodylens.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "progress_photos",
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
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long,
    val photoGroup: PhotoGroup,
    val encryptedFilePath: String, // Path to encrypted photo file
    val thumbnailPath: String? = null, // Path to encrypted thumbnail
    val timestamp: Long = System.currentTimeMillis()
)


