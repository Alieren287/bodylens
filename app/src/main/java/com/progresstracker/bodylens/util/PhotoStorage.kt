package com.progresstracker.bodylens.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for storing and managing photos
 */
object PhotoStorage {

    private const val PHOTOS_DIR = "bodylens_photos"
    private const val QUALITY = 85 // JPEG compression quality

    /**
     * Save a photo to internal storage
     * @param context Application context
     * @param photoData Byte array of photo data
     * @param sessionId ID of the session this photo belongs to
     * @param bodyPartId ID of the body part this photo represents
     * @return File path of saved photo, or null if failed
     */
    fun savePhoto(
        context: Context,
        photoData: ByteArray,
        sessionId: Long,
        bodyPartId: Long
    ): String? {
        return try {
            // Create photos directory if it doesn't exist
            val photosDir = File(context.filesDir, PHOTOS_DIR)
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }

            // Create session subdirectory
            val sessionDir = File(photosDir, "session_$sessionId")
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }

            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "photo_${bodyPartId}_$timestamp.jpg"
            val file = File(sessionDir, filename)

            // Decode, rotate if needed, and save
            val bitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.size)
            val rotatedBitmap = rotateImageIfRequired(bitmap, photoData)

            FileOutputStream(file).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }

            // Clean up
            if (rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            bitmap.recycle()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Delete a photo file
     */
    fun deletePhoto(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete all photos for a session
     */
    fun deleteSessionPhotos(context: Context, sessionId: Long): Boolean {
        return try {
            val photosDir = File(context.filesDir, PHOTOS_DIR)
            val sessionDir = File(photosDir, "session_$sessionId")
            if (sessionDir.exists()) {
                sessionDir.deleteRecursively()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get photo file
     */
    fun getPhotoFile(filePath: String): File? {
        return try {
            val file = File(filePath)
            if (file.exists()) file else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate total storage used by photos
     */
    fun getTotalStorageUsed(context: Context): Long {
        return try {
            val photosDir = File(context.filesDir, PHOTOS_DIR)
            if (photosDir.exists()) {
                photosDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Rotate image based on EXIF orientation
     */
    private fun rotateImageIfRequired(img: Bitmap, photoData: ByteArray): Bitmap {
        return try {
            // Read EXIF data
            val ei = ExifInterface(photoData.inputStream())
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            // Determine rotation angle
            val rotationAngle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            // Return original if no rotation needed
            if (rotationAngle == 0f) {
                return img
            }

            // Rotate bitmap
            val matrix = Matrix()
            matrix.postRotate(rotationAngle)
            Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            img
        }
    }
}
