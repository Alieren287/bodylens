package com.progresstracker.bodylens.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progresstracker.bodylens.data.AppDatabase
import com.progresstracker.bodylens.data.entity.BodyPart
import com.progresstracker.bodylens.data.repository.BodyPartRepository
import com.progresstracker.bodylens.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for managing photo session flow
 */
class PhotoSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val bodyPartRepository = BodyPartRepository(database.bodyPartDao())
    private val sessionRepository = SessionRepository(
        database.sessionDao(),
        database.photoDao(),
        application
    )

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _activeBodyParts = MutableStateFlow<List<BodyPart>>(emptyList())
    val activeBodyParts: StateFlow<List<BodyPart>> = _activeBodyParts.asStateFlow()

    private val _currentBodyPartIndex = MutableStateFlow(0)
    val currentBodyPartIndex: StateFlow<Int> = _currentBodyPartIndex.asStateFlow()

    private val _sessionId = MutableStateFlow<Long?>(null)

    private val _capturedPhotos = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val capturedPhotos: StateFlow<Map<Long, Boolean>> = _capturedPhotos.asStateFlow()

    init {
        loadActiveBodyParts()
    }

    /**
     * Load active body parts
     */
    private fun loadActiveBodyParts() {
        viewModelScope.launch {
            bodyPartRepository.getActiveBodyParts().collect { parts ->
                _activeBodyParts.value = parts
            }
        }
    }

    /**
     * Start a new photo session
     * Note: Session is not created in DB until first photo is captured
     */
    fun startSession() {
        viewModelScope.launch {
            _sessionState.value = SessionState.Loading

            // Get active body parts
            val bodyParts = bodyPartRepository.getActiveBodyParts().first()

            if (bodyParts.isEmpty()) {
                _sessionState.value = SessionState.Error("No active body parts configured")
                return@launch
            }

            // Don't create session in DB yet - wait until first photo
            _sessionId.value = null
            _currentBodyPartIndex.value = 0
            _capturedPhotos.value = emptyMap()
            _sessionState.value = SessionState.InProgress
        }
    }

    /**
     * Get current body part
     */
    fun getCurrentBodyPart(): BodyPart? {
        val parts = _activeBodyParts.value
        val index = _currentBodyPartIndex.value
        return parts.getOrNull(index)
    }

    /**
     * Capture photo for current body part
     */
    fun capturePhoto(photoData: ByteArray) {
        val bodyPart = getCurrentBodyPart() ?: return

        viewModelScope.launch {
            _sessionState.value = SessionState.Saving

            // Create session in DB if this is the first photo
            var sessionId = _sessionId.value
            if (sessionId == null) {
                val result = sessionRepository.createSession()
                if (result.isSuccess) {
                    sessionId = result.getOrNull()!!
                    _sessionId.value = sessionId
                } else {
                    _sessionState.value = SessionState.Error("Failed to create session")
                    return@launch
                }
            }

            sessionRepository.addPhotoToSession(
                sessionId = sessionId,
                bodyPartId = bodyPart.id,
                photoData = photoData
            )
                .onSuccess {
                    // Mark photo as captured
                    val updated = _capturedPhotos.value.toMutableMap()
                    updated[bodyPart.id] = true
                    _capturedPhotos.value = updated

                    // Move to next body part or complete
                    if (isLastBodyPart()) {
                        completeSession()
                    } else {
                        moveToNext()
                    }
                }
                .onFailure { error ->
                    _sessionState.value = SessionState.Error(
                        error.message ?: "Failed to save photo"
                    )
                    // Revert to in progress after error
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(2000)
                        if (_sessionState.value is SessionState.Error) {
                            _sessionState.value = SessionState.InProgress
                        }
                    }
                }
        }
    }

    /**
     * Move to next body part
     */
    fun moveToNext() {
        if (!isLastBodyPart()) {
            _currentBodyPartIndex.value += 1
            _sessionState.value = SessionState.InProgress
        }
    }

    /**
     * Move to previous body part
     */
    fun moveToPrevious() {
        if (_currentBodyPartIndex.value > 0) {
            _currentBodyPartIndex.value -= 1
            _sessionState.value = SessionState.InProgress
        }
    }

    /**
     * Move to specific body part index
     */
    fun moveToIndex(index: Int) {
        if (index >= 0 && index < _activeBodyParts.value.size) {
            _currentBodyPartIndex.value = index
            _sessionState.value = SessionState.InProgress
        }
    }

    /**
     * Skip current body part
     */
    fun skipCurrent() {
        if (isLastBodyPart()) {
            completeSession()
        } else {
            moveToNext()
        }
    }

    /**
     * Check if current is last body part
     */
    private fun isLastBodyPart(): Boolean {
        return _currentBodyPartIndex.value >= _activeBodyParts.value.size - 1
    }

    /**
     * Complete the session
     */
    fun completeSession() {
        val sessionId = _sessionId.value

        // If no session was created (no photos taken), just set state to cancelled
        if (sessionId == null) {
            _sessionState.value = SessionState.Cancelled
            return
        }

        viewModelScope.launch {
            sessionRepository.completeSession(sessionId)
                .onSuccess {
                    _sessionState.value = SessionState.Complete(sessionId)
                }
                .onFailure { error ->
                    _sessionState.value = SessionState.Error(
                        error.message ?: "Failed to complete session"
                    )
                }
        }
    }

    /**
     * Cancel the session
     */
    fun cancelSession() {
        val sessionId = _sessionId.value

        viewModelScope.launch {
            if (sessionId != null) {
                // Delete incomplete session
                sessionRepository.deleteSession(sessionId)
            }
            _sessionState.value = SessionState.Cancelled
        }
    }

    /**
     * Reset session state
     */
    fun resetSession() {
        _sessionState.value = SessionState.Idle
        _currentBodyPartIndex.value = 0
        _sessionId.value = null
        _capturedPhotos.value = emptyMap()
    }

    /**
     * Get progress information
     */
    fun getProgress(): Pair<Int, Int> {
        return Pair(_currentBodyPartIndex.value + 1, _activeBodyParts.value.size)
    }
}

/**
 * Session state
 */
sealed class SessionState {
    object Idle : SessionState()
    object Loading : SessionState()
    object InProgress : SessionState()
    object Saving : SessionState()
    data class Complete(val sessionId: Long) : SessionState()
    object Cancelled : SessionState()
    data class Error(val message: String) : SessionState()
}
