package com.progresstracker.bodylens.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Photo session entity
 * Each session represents a complete set of photos taken at one time
 */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Date/time when session was created
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Optional notes about this session
     */
    val notes: String? = null,

    /**
     * Number of photos in this session
     */
    val photoCount: Int = 0,

    /**
     * Whether this session is complete
     */
    val isComplete: Boolean = false
)
