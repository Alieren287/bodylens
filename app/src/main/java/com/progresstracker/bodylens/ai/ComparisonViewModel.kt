package com.progresstracker.bodylens.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progresstracker.bodylens.data.AppDatabase
import com.progresstracker.bodylens.data.entity.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for AI-powered session comparison
 */
class ComparisonViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val sessionDao = database.sessionDao()
    private val photoDao = database.photoDao()
    private val geminiService = GeminiService()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _selectedOlderSession = MutableStateFlow<Session?>(null)
    val selectedOlderSession: StateFlow<Session?> = _selectedOlderSession.asStateFlow()

    private val _selectedNewerSession = MutableStateFlow<Session?>(null)
    val selectedNewerSession: StateFlow<Session?> = _selectedNewerSession.asStateFlow()

    private val _comparisonResult = MutableStateFlow<String?>(null)
    val comparisonResult: StateFlow<String?> = _comparisonResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSessions()
    }

    /**
     * Load all sessions
     */
    private fun loadSessions() {
        viewModelScope.launch {
            sessionDao.getAllSessions().collect { sessionList ->
                _sessions.value = sessionList
            }
        }
    }

    /**
     * Select older session for comparison
     */
    fun selectOlderSession(session: Session) {
        _selectedOlderSession.value = session
        _error.value = null
    }

    /**
     * Select newer session for comparison
     */
    fun selectNewerSession(session: Session) {
        _selectedNewerSession.value = session
        _error.value = null
    }

    /**
     * Compare the two selected sessions using AI
     */
    fun compareSelectedSessions() {
        val older = _selectedOlderSession.value
        val newer = _selectedNewerSession.value

        if (older == null || newer == null) {
            _error.value = "Please select two sessions to compare"
            return
        }

        if (older.id == newer.id) {
            _error.value = "Please select two different sessions"
            return
        }

        // Ensure older is actually older
        val (earlierSession, laterSession) = if (older.timestamp < newer.timestamp) {
            older to newer
        } else {
            newer to older
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _comparisonResult.value = null

            try {
                // Load photos for both sessions
                val olderPhotos = photoDao.getPhotosForSession(earlierSession.id)
                    .first()
                    .map { photo -> photo.filePath }

                val newerPhotos = photoDao.getPhotosForSession(laterSession.id)
                    .first()
                    .map { photo -> photo.filePath }

                if (olderPhotos.isEmpty() || newerPhotos.isEmpty()) {
                    _error.value = "One or both sessions have no photos"
                    _isLoading.value = false
                    return@launch
                }

                // Call Gemini API
                val result = geminiService.compareProgressPhotos(olderPhotos, newerPhotos)

                result.onSuccess { analysis ->
                    _comparisonResult.value = analysis
                }.onFailure { exception ->
                    _error.value = exception.message ?: "Failed to analyze photos"
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear selection and results
     */
    fun clearComparison() {
        _selectedOlderSession.value = null
        _selectedNewerSession.value = null
        _comparisonResult.value = null
        _error.value = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
