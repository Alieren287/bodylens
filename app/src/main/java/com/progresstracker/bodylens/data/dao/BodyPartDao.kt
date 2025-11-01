package com.progresstracker.bodylens.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.progresstracker.bodylens.data.entity.BodyPart
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for BodyPart operations
 */
@Dao
interface BodyPartDao {

    /**
     * Get all body parts ordered by display order
     */
    @Query("SELECT * FROM body_parts ORDER BY `order` ASC")
    fun getAllBodyParts(): Flow<List<BodyPart>>

    /**
     * Get all active body parts ordered by display order
     */
    @Query("SELECT * FROM body_parts WHERE isActive = 1 ORDER BY `order` ASC")
    fun getActiveBodyParts(): Flow<List<BodyPart>>

    /**
     * Get body part by ID
     */
    @Query("SELECT * FROM body_parts WHERE id = :id")
    suspend fun getBodyPartById(id: Long): BodyPart?

    /**
     * Insert a new body part
     */
    @Insert
    suspend fun insert(bodyPart: BodyPart): Long

    /**
     * Insert multiple body parts
     */
    @Insert
    suspend fun insertAll(bodyParts: List<BodyPart>)

    /**
     * Update an existing body part
     */
    @Update
    suspend fun update(bodyPart: BodyPart)

    /**
     * Delete a body part
     */
    @Delete
    suspend fun delete(bodyPart: BodyPart)

    /**
     * Delete a body part by ID
     */
    @Query("DELETE FROM body_parts WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Get count of body parts
     */
    @Query("SELECT COUNT(*) FROM body_parts")
    suspend fun getCount(): Int

    /**
     * Update body part order
     */
    @Query("UPDATE body_parts SET `order` = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Int)

    /**
     * Toggle body part active status
     */
    @Query("UPDATE body_parts SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)
}
