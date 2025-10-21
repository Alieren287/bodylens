package com.example.bodylens.data.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class EncryptedFileManager(private val context: Context) {
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private fun getPhotosDirectory(): File {
        val photosDir = File(context.filesDir, "encrypted_photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        return photosDir
    }
    
    private fun getThumbnailsDirectory(): File {
        val thumbsDir = File(context.filesDir, "encrypted_thumbnails")
        if (!thumbsDir.exists()) {
            thumbsDir.mkdirs()
        }
        return thumbsDir
    }
    
    /**
     * Save a photo with encryption
     * @return Path to the encrypted file
     */
    suspend fun saveEncryptedPhoto(bitmap: Bitmap, fileName: String): String {
        val file = File(getPhotosDirectory(), fileName)
        val encryptedFile = createEncryptedFile(file)
        
        val outputStream = encryptedFile.openFileOutput()
        val byteArrayOutputStream = ByteArrayOutputStream()
        
        // Compress bitmap to JPEG
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val bitmapData = byteArrayOutputStream.toByteArray()
        
        outputStream.write(bitmapData)
        outputStream.flush()
        outputStream.close()
        
        return file.absolutePath
    }
    
    /**
     * Save a thumbnail version of the photo
     * @return Path to the encrypted thumbnail file
     */
    suspend fun saveThumbnail(bitmap: Bitmap, fileName: String): String {
        val file = File(getThumbnailsDirectory(), fileName)
        val encryptedFile = createEncryptedFile(file)
        
        // Create thumbnail (reduced size)
        val thumbnailSize = 200
        val scale = minOf(
            thumbnailSize.toFloat() / bitmap.width,
            thumbnailSize.toFloat() / bitmap.height
        )
        
        val thumbnailWidth = (bitmap.width * scale).toInt()
        val thumbnailHeight = (bitmap.height * scale).toInt()
        
        val thumbnail = Bitmap.createScaledBitmap(bitmap, thumbnailWidth, thumbnailHeight, true)
        
        val outputStream = encryptedFile.openFileOutput()
        val byteArrayOutputStream = ByteArrayOutputStream()
        
        // Compress thumbnail to JPEG with lower quality
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val thumbnailData = byteArrayOutputStream.toByteArray()
        
        outputStream.write(thumbnailData)
        outputStream.flush()
        outputStream.close()
        
        thumbnail.recycle()
        
        return file.absolutePath
    }
    
    /**
     * Load a decrypted photo as Bitmap
     */
    suspend fun loadDecryptedPhoto(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            
            val encryptedFile = createEncryptedFile(file)
            val inputStream = encryptedFile.openFileInput()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Delete an encrypted file
     */
    suspend fun deleteEncryptedFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get file size in bytes
     */
    fun getFileSize(filePath: String): Long {
        val file = File(filePath)
        return if (file.exists()) file.length() else 0
    }
    
    /**
     * Export encrypted photo to external storage (for backup)
     */
    suspend fun exportPhoto(filePath: String, destinationPath: String): Boolean {
        return try {
            val sourceFile = File(filePath)
            val destinationFile = File(destinationPath)
            
            if (!sourceFile.exists()) return false
            
            val encryptedFile = createEncryptedFile(sourceFile)
            val inputStream = encryptedFile.openFileInput()
            val outputStream = FileOutputStream(destinationFile)
            
            inputStream.copyTo(outputStream)
            
            inputStream.close()
            outputStream.close()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get total storage used by encrypted files
     */
    fun getTotalStorageUsed(): Long {
        var totalSize = 0L
        
        getPhotosDirectory().listFiles()?.forEach { file ->
            totalSize += file.length()
        }
        
        getThumbnailsDirectory().listFiles()?.forEach { file ->
            totalSize += file.length()
        }
        
        return totalSize
    }
    
    /**
     * Delete all encrypted files (for app reset)
     */
    suspend fun deleteAllFiles() {
        getPhotosDirectory().listFiles()?.forEach { it.delete() }
        getThumbnailsDirectory().listFiles()?.forEach { it.delete() }
    }
    
    private fun createEncryptedFile(file: File): EncryptedFile {
        return EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }
    
    /**
     * Generate unique filename for photo
     */
    fun generatePhotoFileName(entryId: Long, photoGroup: String): String {
        val timestamp = System.currentTimeMillis()
        return "photo_${entryId}_${photoGroup}_${timestamp}.enc"
    }
    
    /**
     * Generate unique filename for thumbnail
     */
    fun generateThumbnailFileName(entryId: Long, photoGroup: String): String {
        val timestamp = System.currentTimeMillis()
        return "thumb_${entryId}_${photoGroup}_${timestamp}.enc"
    }
}



