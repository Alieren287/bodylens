package com.example.bodylens.data.local.dao

import androidx.room.*
import com.example.bodylens.data.model.ProgressEntry
import com.example.bodylens.data.model.ProgressEntryWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressEntryDao {
    
    @Query("SELECT * FROM progress_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<ProgressEntry>>
    
    @Query("SELECT * FROM progress_entries WHERE id = :entryId")
    suspend fun getEntryById(entryId: Long): ProgressEntry?
    
    @Transaction
    @Query("SELECT * FROM progress_entries WHERE id = :entryId")
    suspend fun getEntryWithDetails(entryId: Long): ProgressEntryWithDetails?
    
    @Transaction
    @Query("SELECT * FROM progress_entries ORDER BY timestamp DESC")
    fun getAllEntriesWithDetails(): Flow<List<ProgressEntryWithDetails>>
    
    @Query("SELECT * FROM progress_entries WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<ProgressEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ProgressEntry): Long
    
    @Update
    suspend fun updateEntry(entry: ProgressEntry)
    
    @Delete
    suspend fun deleteEntry(entry: ProgressEntry)
    
    @Query("DELETE FROM progress_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)
    
    @Query("SELECT COUNT(*) FROM progress_entries")
    suspend fun getEntryCount(): Int
}


