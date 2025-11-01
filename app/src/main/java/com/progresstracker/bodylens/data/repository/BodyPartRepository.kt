package com.progresstracker.bodylens.data.repository

import com.progresstracker.bodylens.data.dao.BodyPartDao
import com.progresstracker.bodylens.data.entity.BodyPart
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing body parts
 */
class BodyPartRepository(private val bodyPartDao: BodyPartDao) {

    /**
     * Get all body parts
     */
    fun getAllBodyParts(): Flow<List<BodyPart>> {
        return bodyPartDao.getAllBodyParts()
    }

    /**
     * Get only active body parts
     */
    fun getActiveBodyParts(): Flow<List<BodyPart>> {
        return bodyPartDao.getActiveBodyParts()
    }

    /**
     * Get body part by ID
     */
    suspend fun getBodyPartById(id: Long): BodyPart? {
        return bodyPartDao.getBodyPartById(id)
    }

    /**
     * Add a new body part
     */
    suspend fun addBodyPart(name: String, icon: String = "person"): Result<Long> {
        return try {
            // Get current max order
            val allParts = bodyPartDao.getAllBodyParts()
            var maxOrder = -1

            // Since Flow is async, we need to get the current value
            // For simplicity, we'll just use the count
            val count = bodyPartDao.getCount()
            maxOrder = count

            val bodyPart = BodyPart(
                name = name,
                order = maxOrder,
                icon = icon,
                isDefault = false,
                isActive = true
            )
            val id = bodyPartDao.insert(bodyPart)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add a default body part (like Face)
     */
    suspend fun addDefaultBodyPart(name: String, icon: String = "person"): Result<Long> {
        return try {
            // Get current max order
            val count = bodyPartDao.getCount()

            val bodyPart = BodyPart(
                name = name,
                order = count,
                icon = icon,
                isDefault = true,  // Mark as default
                isActive = true
            )
            val id = bodyPartDao.insert(bodyPart)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing body part
     */
    suspend fun updateBodyPart(bodyPart: BodyPart): Result<Unit> {
        return try {
            bodyPartDao.update(bodyPart)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a body part (only if not default)
     */
    suspend fun deleteBodyPart(bodyPart: BodyPart): Result<Unit> {
        return try {
            if (bodyPart.isDefault) {
                Result.failure(IllegalArgumentException("Cannot delete default body part"))
            } else {
                bodyPartDao.delete(bodyPart)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle body part active status
     */
    suspend fun toggleActive(id: Long, isActive: Boolean): Result<Unit> {
        return try {
            bodyPartDao.setActive(id, isActive)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reorder body parts
     */
    suspend fun reorderBodyParts(bodyParts: List<BodyPart>): Result<Unit> {
        return try {
            bodyParts.forEachIndexed { index, bodyPart ->
                bodyPartDao.updateOrder(bodyPart.id, index)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get count of body parts
     */
    suspend fun getCount(): Int {
        return bodyPartDao.getCount()
    }
}
