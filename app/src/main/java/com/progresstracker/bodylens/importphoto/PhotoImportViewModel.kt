package com.progresstracker.bodylens.importphoto

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progresstracker.bodylens.data.AppDatabase
import com.progresstracker.bodylens.data.entity.BodyPart
import com.progresstracker.bodylens.data.entity.Photo
import com.progresstracker.bodylens.data.entity.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Data class to hold imported photo info
 */
data class ImportedPhoto(
    val uri: Uri,
    val bodyPartId: Long? = null,
    val detectedDate: Long? = null
)

/**
 * ViewModel for photo import functionality
 */
class PhotoImportViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val sessionDao = database.sessionDao()
    private val photoDao = database.photoDao()
    private val bodyPartDao = database.bodyPartDao()

    private val _bodyParts = MutableStateFlow<List<BodyPart>>(emptyList())
    val bodyParts: StateFlow<List<BodyPart>> = _bodyParts.asStateFlow()

    private val _selectedPhotos = MutableStateFlow<List<ImportedPhoto>>(emptyList())
    val selectedPhotos: StateFlow<List<ImportedPhoto>> = _selectedPhotos.asStateFlow()

    private val _selectedDate = MutableStateFlow<Long>(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _importComplete = MutableStateFlow(false)
    val importComplete: StateFlow<Boolean> = _importComplete.asStateFlow()

    init {
        loadBodyParts()
    }

    /**
     * Load available body parts
     */
    private fun loadBodyParts() {
        viewModelScope.launch {
            bodyPartDao.getAllBodyParts().collect { parts ->
                _bodyParts.value = parts.filter { it.isActive }
            }
        }
    }

    /**
     * Add selected photos from URIs
     */
    fun addPhotos(uris: List<Uri>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val newPhotos = uris.map { uri ->
                val detectedDate = extractDateFromExif(context, uri)
                ImportedPhoto(
                    uri = uri,
                    detectedDate = detectedDate
                )
            }

            _selectedPhotos.value = _selectedPhotos.value + newPhotos

            // If we detected a date from the first photo, use it as the session date
            newPhotos.firstOrNull()?.detectedDate?.let { date ->
                _selectedDate.value = date
            }
        }
    }

    /**
     * Remove a photo from the selection
     */
    fun removePhoto(photo: ImportedPhoto) {
        _selectedPhotos.value = _selectedPhotos.value.filter { it != photo }
    }

    /**
     * Assign a body part to a photo
     */
    fun assignBodyPart(photo: ImportedPhoto, bodyPartId: Long) {
        _selectedPhotos.value = _selectedPhotos.value.map {
            if (it.uri == photo.uri) {
                it.copy(bodyPartId = bodyPartId)
            } else {
                it
            }
        }
    }

    /**
     * Set the session date
     */
    fun setSessionDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }

    /**
     * Import the photos to the database
     */
    fun importPhotos(onComplete: () -> Unit) {
        val photos = _selectedPhotos.value

        if (photos.isEmpty()) {
            _error.value = "Please select at least one photo"
            return
        }

        // Check if all photos have body parts assigned
        val unassigned = photos.filter { it.bodyPartId == null }
        if (unassigned.isNotEmpty()) {
            _error.value = "Please assign body parts to all photos"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            try {
                val context = getApplication<Application>()

                // Create a new session
                val session = Session(
                    timestamp = _selectedDate.value,
                    photoCount = photos.size,
                    isComplete = true
                )

                val sessionId = sessionDao.insert(session)

                // Copy photos to app storage and insert into database
                photos.forEach { importedPhoto ->
                    val savedPath = copyPhotoToStorage(context, importedPhoto.uri, sessionId)

                    if (savedPath != null) {
                        val photo = Photo(
                            sessionId = sessionId,
                            bodyPartId = importedPhoto.bodyPartId!!,
                            filePath = savedPath,
                            timestamp = _selectedDate.value
                        )
                        photoDao.insert(photo)
                    }
                }

                _importComplete.value = true
                onComplete()

            } catch (e: Exception) {
                _error.value = "Failed to import photos: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Extract date from EXIF data
     */
    private fun extractDateFromExif(context: Context, uri: Uri): Long? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use {
                val exif = ExifInterface(it)
                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)

                dateTime?.let { dateStr ->
                    val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                    format.parse(dateStr)?.time
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copy photo from URI to app storage
     */
    private fun copyPhotoToStorage(context: Context, uri: Uri, sessionId: Long): String? {
        return try {
            val photosDir = File(context.filesDir, "photos")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }

            val fileName = "session_${sessionId}_${System.currentTimeMillis()}.jpg"
            val destFile = File(photosDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset state
     */
    fun reset() {
        _selectedPhotos.value = emptyList()
        _selectedDate.value = System.currentTimeMillis()
        _error.value = null
        _importComplete.value = false
    }
}
