package com.progresstracker.bodylens.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.progresstracker.bodylens.data.entity.Session
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Session operations
 */
@Dao
interface SessionDao {

    /**
     * Get all sessions ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<Session>>

    /**
     * Get session by ID
     */
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): Session?

    /**
     * Get session by ID as Flow
     */
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getSessionByIdFlow(id: Long): Flow<Session?>

    /**
     * Insert a new session
     */
    @Insert
    suspend fun insert(session: Session): Long

    /**
     * Update an existing session
     */
    @Update
    suspend fun update(session: Session)

    /**
     * Delete a session (will cascade delete photos)
     */
    @Delete
    suspend fun delete(session: Session)

    /**
     * Delete a session by ID
     */
    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Get count of sessions
     */
    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getCount(): Int

    /**
     * Update session photo count
     */
    @Query("UPDATE sessions SET photoCount = :count WHERE id = :id")
    suspend fun updatePhotoCount(id: Long, count: Int)

    /**
     * Mark session as complete
     */
    @Query("UPDATE sessions SET isComplete = :isComplete WHERE id = :id")
    suspend fun setComplete(id: Long, isComplete: Boolean)
}
