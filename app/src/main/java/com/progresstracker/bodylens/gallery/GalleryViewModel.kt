package com.progresstracker.bodylens.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progresstracker.bodylens.data.AppDatabase
import com.progresstracker.bodylens.data.entity.Photo
import com.progresstracker.bodylens.data.entity.Session
import com.progresstracker.bodylens.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for gallery/session list
 */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val sessionRepository = SessionRepository(
        database.sessionDao(),
        database.photoDao(),
        application
    )

    private val _sessions = MutableStateFlow<List<SessionWithPhotos>>(emptyList())
    val sessions: StateFlow<List<SessionWithPhotos>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSessions()
    }

    /**
     * Load all sessions with their photos
     */
    private fun loadSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            sessionRepository.getAllSessions().collect { sessionList ->
                // Just use the photo count from the session entity
                // Don't need to load actual photos for the list view
                val sessionsWithPhotos = sessionList.map { session ->
                    SessionWithPhotos(session, emptyList()) // We'll use session.photoCount instead
                }
                _sessions.value = sessionsWithPhotos
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a session
     */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
    }
}

/**
 * Session with its photos
 */
data class SessionWithPhotos(
    val session: Session,
    val photos: List<Photo>
)
