package com.progresstracker.bodylens.data.repository

import android.content.Context
import com.progresstracker.bodylens.data.dao.PhotoDao
import com.progresstracker.bodylens.data.dao.SessionDao
import com.progresstracker.bodylens.data.entity.Photo
import com.progresstracker.bodylens.data.entity.Session
import com.progresstracker.bodylens.util.PhotoStorage
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing photo sessions
 */
class SessionRepository(
    private val sessionDao: SessionDao,
    private val photoDao: PhotoDao,
    private val context: Context
) {

    /**
     * Get all sessions
     */
    fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions()
    }

    /**
     * Get session by ID
     */
    suspend fun getSessionById(id: Long): Session? {
        return sessionDao.getSessionById(id)
    }

    /**
     * Get session by ID as Flow
     */
    fun getSessionByIdFlow(id: Long): Flow<Session?> {
        return sessionDao.getSessionByIdFlow(id)
    }

    /**
     * Create a new session
     */
    suspend fun createSession(notes: String? = null): Result<Long> {
        return try {
            val session = Session(
                notes = notes,
                photoCount = 0,
                isComplete = false
            )
            val sessionId = sessionDao.insert(session)
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add photo to session
     */
    suspend fun addPhotoToSession(
        sessionId: Long,
        bodyPartId: Long,
        photoData: ByteArray,
        notes: String? = null
    ): Result<Long> {
        return try {
            // Save photo to storage
            val filePath = PhotoStorage.savePhoto(context, photoData, sessionId, bodyPartId)
                ?: return Result.failure(Exception("Failed to save photo"))

            // Create photo entity
            val photo = Photo(
                sessionId = sessionId,
                bodyPartId = bodyPartId,
                filePath = filePath,
                notes = notes
            )

            // Insert into database
            val photoId = photoDao.insert(photo)

            // Update session photo count
            val count = photoDao.getCountForSession(sessionId)
            sessionDao.updatePhotoCount(sessionId, count)

            Result.success(photoId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all photos for a session
     */
    fun getPhotosForSession(sessionId: Long): Flow<List<Photo>> {
        return photoDao.getPhotosForSession(sessionId)
    }

    /**
     * Get photo for specific session and body part
     */
    suspend fun getPhotoForSessionAndBodyPart(sessionId: Long, bodyPartId: Long): Photo? {
        return photoDao.getPhotoForSessionAndBodyPart(sessionId, bodyPartId)
    }

    /**
     * Mark session as complete
     */
    suspend fun completeSession(sessionId: Long): Result<Unit> {
        return try {
            sessionDao.setComplete(sessionId, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update session notes
     */
    suspend fun updateSessionNotes(sessionId: Long, notes: String): Result<Unit> {
        return try {
            val session = sessionDao.getSessionById(sessionId)
                ?: return Result.failure(Exception("Session not found"))
            sessionDao.update(session.copy(notes = notes))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete session and all its photos
     */
    suspend fun deleteSession(sessionId: Long): Result<Unit> {
        return try {
            // Delete photos from storage
            PhotoStorage.deleteSessionPhotos(context, sessionId)

            // Delete from database (cascade deletes photos)
            sessionDao.deleteById(sessionId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a specific photo
     */
    suspend fun deletePhoto(photoId: Long): Result<Unit> {
        return try {
            val photo = photoDao.getPhotoById(photoId)
                ?: return Result.failure(Exception("Photo not found"))

            // Delete file
            PhotoStorage.deletePhoto(photo.filePath)

            // Delete from database
            photoDao.deleteById(photoId)

            // Update session photo count
            val count = photoDao.getCountForSession(photo.sessionId)
            sessionDao.updatePhotoCount(photo.sessionId, count)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get total storage used
     */
    fun getTotalStorageUsed(): Long {
        return PhotoStorage.getTotalStorageUsed(context)
    }
}
