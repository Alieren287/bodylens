package com.example.bodylens.data.local.dao

import androidx.room.*
import com.example.bodylens.data.model.Measurement
import com.example.bodylens.data.model.MeasurementType
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    
    @Query("SELECT * FROM measurements WHERE entryId = :entryId")
    fun getMeasurementsForEntry(entryId: Long): Flow<List<Measurement>>
    
    @Query("SELECT * FROM measurements WHERE type = :type ORDER BY timestamp DESC")
    fun getMeasurementsByType(type: MeasurementType): Flow<List<Measurement>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: Measurement): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(measurements: List<Measurement>)
    
    @Delete
    suspend fun deleteMeasurement(measurement: Measurement)
    
    @Query("DELETE FROM measurements WHERE entryId = :entryId")
    suspend fun deleteMeasurementsForEntry(entryId: Long)
}


