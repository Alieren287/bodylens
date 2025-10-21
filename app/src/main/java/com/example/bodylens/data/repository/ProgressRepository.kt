package com.example.bodylens.data.repository

import android.graphics.Bitmap
import com.example.bodylens.data.local.BodyLensDatabase
import com.example.bodylens.data.model.*
import com.example.bodylens.data.security.EncryptedFileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProgressRepository(
    private val database: BodyLensDatabase,
    private val encryptedFileManager: EncryptedFileManager
) {
    
    private val progressEntryDao = database.progressEntryDao()
    private val progressPhotoDao = database.progressPhotoDao()
    private val measurementDao = database.measurementDao()
    private val aiInsightDao = database.aiInsightDao()
    
    // Progress Entries
    fun getAllEntries(): Flow<List<ProgressEntry>> = progressEntryDao.getAllEntries()
    
    fun getAllEntriesWithDetails(): Flow<List<ProgressEntryWithDetails>> = 
        progressEntryDao.getAllEntriesWithDetails()
    
    suspend fun getEntryById(entryId: Long): ProgressEntry? = 
        progressEntryDao.getEntryById(entryId)
    
    suspend fun getEntryWithDetails(entryId: Long): ProgressEntryWithDetails? = 
        progressEntryDao.getEntryWithDetails(entryId)
    
    suspend fun createEntry(entry: ProgressEntry): Long = 
        progressEntryDao.insertEntry(entry)
    
    suspend fun updateEntry(entry: ProgressEntry) = 
        progressEntryDao.updateEntry(entry)
    
    suspend fun deleteEntry(entryId: Long) {
        // Delete associated photos from filesystem
        // Use first() to get a single emission from the Flow
        val photos = progressPhotoDao.getPhotosForEntry(entryId)
        try {
            val photoList = photos.first()
            photoList.forEach { photo ->
                encryptedFileManager.deleteEncryptedFile(photo.encryptedFilePath)
                photo.thumbnailPath?.let { 
                    encryptedFileManager.deleteEncryptedFile(it) 
                }
            }
        } catch (e: Exception) {
            // If no photos exist, just continue to delete the entry
            e.printStackTrace()
        }
        
        // Delete from database (cascade will handle related records)
        progressEntryDao.deleteEntryById(entryId)
    }
    
    fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<ProgressEntry>> = 
        progressEntryDao.getEntriesByDateRange(startTime, endTime)
    
    suspend fun getEntryCount(): Int = progressEntryDao.getEntryCount()
    
    // Progress Photos
    fun getPhotosForEntry(entryId: Long): Flow<List<ProgressPhoto>> = 
        progressPhotoDao.getPhotosForEntry(entryId)
    
    fun getPhotosByGroup(group: PhotoGroup): Flow<List<ProgressPhoto>> = 
        progressPhotoDao.getPhotosByGroup(group)
    
    suspend fun savePhoto(
        entryId: Long,
        photoGroup: PhotoGroup,
        bitmap: Bitmap
    ): Long {
        val photoFileName = encryptedFileManager.generatePhotoFileName(entryId, photoGroup.name)
        val thumbnailFileName = encryptedFileManager.generateThumbnailFileName(entryId, photoGroup.name)
        
        val photoPath = encryptedFileManager.saveEncryptedPhoto(bitmap, photoFileName)
        val thumbnailPath = encryptedFileManager.saveThumbnail(bitmap, thumbnailFileName)
        
        val photo = ProgressPhoto(
            entryId = entryId,
            photoGroup = photoGroup,
            encryptedFilePath = photoPath,
            thumbnailPath = thumbnailPath
        )
        
        return progressPhotoDao.insertPhoto(photo)
    }
    
    suspend fun loadPhotoImage(photo: ProgressPhoto): Bitmap? = 
        encryptedFileManager.loadDecryptedPhoto(photo.encryptedFilePath)
    
    suspend fun loadThumbnailImage(photo: ProgressPhoto): Bitmap? = 
        photo.thumbnailPath?.let { encryptedFileManager.loadDecryptedPhoto(it) }
    
    suspend fun deletePhoto(photoId: Long) {
        val photo = progressPhotoDao.getPhotoById(photoId)
        photo?.let {
            encryptedFileManager.deleteEncryptedFile(it.encryptedFilePath)
            it.thumbnailPath?.let { path -> 
                encryptedFileManager.deleteEncryptedFile(path) 
            }
            progressPhotoDao.deletePhotoById(photoId)
        }
    }
    
    // Measurements
    fun getMeasurementsForEntry(entryId: Long): Flow<List<Measurement>> = 
        measurementDao.getMeasurementsForEntry(entryId)
    
    fun getMeasurementsByType(type: MeasurementType): Flow<List<Measurement>> = 
        measurementDao.getMeasurementsByType(type)
    
    suspend fun saveMeasurement(measurement: Measurement): Long = 
        measurementDao.insertMeasurement(measurement)
    
    suspend fun saveMeasurements(measurements: List<Measurement>) = 
        measurementDao.insertMeasurements(measurements)
    
    suspend fun deleteMeasurement(measurement: Measurement) = 
        measurementDao.deleteMeasurement(measurement)
    
    // AI Insights
    fun getInsightsForEntry(entryId: Long): Flow<List<AIInsight>> = 
        aiInsightDao.getInsightsForEntry(entryId)
    
    fun getInsightsByType(type: InsightType): Flow<List<AIInsight>> = 
        aiInsightDao.getInsightsByType(type)
    
    fun getRecentInsights(limit: Int = 20): Flow<List<AIInsight>> = 
        aiInsightDao.getRecentInsights(limit)
    
    suspend fun saveInsight(insight: AIInsight): Long = 
        aiInsightDao.insertInsight(insight)
    
    suspend fun saveInsights(insights: List<AIInsight>) = 
        aiInsightDao.insertInsights(insights)
    
    suspend fun deleteInsight(insight: AIInsight) = 
        aiInsightDao.deleteInsight(insight)
    
    // Storage management
    fun getTotalStorageUsed(): Long = encryptedFileManager.getTotalStorageUsed()
    
    suspend fun deleteAllData() {
        encryptedFileManager.deleteAllFiles()
        // Database will be handled by closing and recreating
    }
}



