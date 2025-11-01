package com.progresstracker.bodylens.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.progresstracker.bodylens.data.entity.Photo
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Photo operations
 */
@Dao
interface PhotoDao {

    /**
     * Get all photos for a session
     */
    @Query("SELECT * FROM photos WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getPhotosForSession(sessionId: Long): Flow<List<Photo>>

    /**
     * Get all photos for a body part
     */
    @Query("SELECT * FROM photos WHERE bodyPartId = :bodyPartId ORDER BY timestamp DESC")
    fun getPhotosForBodyPart(bodyPartId: Long): Flow<List<Photo>>

    /**
     * Get photo by ID
     */
    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): Photo?

    /**
     * Get photo for specific session and body part
     */
    @Query("SELECT * FROM photos WHERE sessionId = :sessionId AND bodyPartId = :bodyPartId LIMIT 1")
    suspend fun getPhotoForSessionAndBodyPart(sessionId: Long, bodyPartId: Long): Photo?

    /**
     * Insert a new photo
     */
    @Insert
    suspend fun insert(photo: Photo): Long

    /**
     * Update an existing photo
     */
    @Update
    suspend fun update(photo: Photo)

    /**
     * Delete a photo
     */
    @Delete
    suspend fun delete(photo: Photo)

    /**
     * Delete a photo by ID
     */
    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Get count of photos for a session
     */
    @Query("SELECT COUNT(*) FROM photos WHERE sessionId = :sessionId")
    suspend fun getCountForSession(sessionId: Long): Int

    /**
     * Get all photos (for all sessions)
     */
    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<Photo>>
}
