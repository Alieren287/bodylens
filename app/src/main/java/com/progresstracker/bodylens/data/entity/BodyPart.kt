package com.progresstracker.bodylens.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Body part entity for organizing photos
 * Users can configure which body parts they want to track
 */
@Entity(tableName = "body_parts")
data class BodyPart(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Name of the body part (e.g., "Front", "Back", "Left Side")
     */
    val name: String,

    /**
     * Display order (lower numbers appear first)
     */
    val order: Int,

    /**
     * Icon name for display (Material Icons name)
     */
    val icon: String = "person",

    /**
     * Whether this is a default body part (cannot be deleted)
     */
    val isDefault: Boolean = false,

    /**
     * Whether this body part is active/enabled
     */
    val isActive: Boolean = true,

    /**
     * Timestamp when created
     */
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Default body parts to be created on first launch
 */
object DefaultBodyParts {
    val defaults = listOf(
        BodyPart(
            name = "Face",
            order = 0,
            icon = "face",
            isDefault = true
        ),
        BodyPart(
            name = "Front",
            order = 1,
            icon = "person",
            isDefault = true
        ),
        BodyPart(
            name = "Back",
            order = 2,
            icon = "person_outline",
            isDefault = true
        ),
        BodyPart(
            name = "Left Side",
            order = 3,
            icon = "accessibility_new",
            isDefault = true
        ),
        BodyPart(
            name = "Right Side",
            order = 4,
            icon = "accessibility_new",
            isDefault = true
        )
    )
}
