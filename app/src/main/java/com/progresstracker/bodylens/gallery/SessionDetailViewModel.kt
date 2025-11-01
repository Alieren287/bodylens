package com.progresstracker.bodylens.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.progresstracker.bodylens.data.AppDatabase
import com.progresstracker.bodylens.data.entity.BodyPart
import com.progresstracker.bodylens.data.entity.Photo
import com.progresstracker.bodylens.data.entity.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for session detail screen
 */
class SessionDetailViewModel(
    application: Application,
    private val sessionId: Long
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val sessionDao = database.sessionDao()
    private val photoDao = database.photoDao()
    private val bodyPartDao = database.bodyPartDao()

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _photosWithBodyParts = MutableStateFlow<List<PhotoWithBodyPart>>(emptyList())
    val photosWithBodyParts: StateFlow<List<PhotoWithBodyPart>> = _photosWithBodyParts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSessionDetails()
    }

    /**
     * Load session details and photos
     */
    private fun loadSessionDetails() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Load session
                _session.value = sessionDao.getSessionById(sessionId)

                // Load photos and body parts together
                combine(
                    photoDao.getPhotosForSession(sessionId),
                    bodyPartDao.getAllBodyParts()
                ) { photos, bodyParts ->
                    // Create a map for quick lookup
                    val bodyPartMap = bodyParts.associateBy { it.id }

                    // Combine photos with their body part info
                    photos.map { photo ->
                        PhotoWithBodyPart(
                            photo = photo,
                            bodyPart = bodyPartMap[photo.bodyPartId]
                        )
                    }
                }.collect { combined ->
                    _photosWithBodyParts.value = combined
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                _photosWithBodyParts.value = emptyList()
            }
        }
    }
}

/**
 * Photo with its body part information
 */
data class PhotoWithBodyPart(
    val photo: Photo,
    val bodyPart: BodyPart?
)

/**
 * Factory for creating SessionDetailViewModel with parameters
 */
class SessionDetailViewModelFactory(
    private val application: Application,
    private val sessionId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionDetailViewModel::class.java)) {
            return SessionDetailViewModel(application, sessionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
