package com.progresstracker.bodylens.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Photo entity
 * Each photo belongs to a session and a body part
 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BodyPart::class,
            parentColumns = ["id"],
            childColumns = ["bodyPartId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["bodyPartId"])
    ]
)
data class Photo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Session this photo belongs to
     */
    val sessionId: Long,

    /**
     * Body part this photo represents
     */
    val bodyPartId: Long,

    /**
     * File path to the photo on device storage
     */
    val filePath: String,

    /**
     * Timestamp when photo was taken
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Optional notes about this specific photo
     */
    val notes: String? = null
)
