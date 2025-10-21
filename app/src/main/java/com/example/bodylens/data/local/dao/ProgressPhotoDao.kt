package com.example.bodylens.data.local.dao

import androidx.room.*
import com.example.bodylens.data.model.PhotoGroup
import com.example.bodylens.data.model.ProgressPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressPhotoDao {
    
    @Query("SELECT * FROM progress_photos WHERE entryId = :entryId")
    fun getPhotosForEntry(entryId: Long): Flow<List<ProgressPhoto>>
    
    @Query("SELECT * FROM progress_photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: Long): ProgressPhoto?
    
    @Query("SELECT * FROM progress_photos WHERE photoGroup = :group ORDER BY timestamp DESC")
    fun getPhotosByGroup(group: PhotoGroup): Flow<List<ProgressPhoto>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: ProgressPhoto): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<ProgressPhoto>)
    
    @Delete
    suspend fun deletePhoto(photo: ProgressPhoto)
    
    @Query("DELETE FROM progress_photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: Long)
    
    @Query("DELETE FROM progress_photos WHERE entryId = :entryId")
    suspend fun deletePhotosForEntry(entryId: Long)
}


