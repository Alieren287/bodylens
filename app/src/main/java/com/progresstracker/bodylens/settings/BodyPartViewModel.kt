package com.progresstracker.bodylens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progresstracker.bodylens.data.AppDatabase
import com.progresstracker.bodylens.data.entity.BodyPart
import com.progresstracker.bodylens.data.repository.BodyPartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for body part configuration
 */
class BodyPartViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BodyPartRepository
    private val _bodyParts = MutableStateFlow<List<BodyPart>>(emptyList())
    val bodyParts: StateFlow<List<BodyPart>> = _bodyParts.asStateFlow()

    private val _uiState = MutableStateFlow<BodyPartUiState>(BodyPartUiState.Idle)
    val uiState: StateFlow<BodyPartUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BodyPartRepository(database.bodyPartDao())
        loadBodyParts()
    }

    /**
     * Load all body parts
     */
    private fun loadBodyParts() {
        viewModelScope.launch {
            repository.getAllBodyParts().collect { parts ->
                _bodyParts.value = parts
            }
        }
    }

    /**
     * Add a new body part
     */
    fun addBodyPart(name: String, icon: String = "person") {
        if (name.isBlank()) {
            _uiState.value = BodyPartUiState.Error("Name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = BodyPartUiState.Loading

            repository.addBodyPart(name, icon)
                .onSuccess {
                    _uiState.value = BodyPartUiState.Success("Body part added")
                }
                .onFailure { error ->
                    _uiState.value = BodyPartUiState.Error(
                        error.message ?: "Failed to add body part"
                    )
                }
        }
    }

    /**
     * Add Face as a default body part (for existing users)
     */
    fun addFaceAsDefault() {
        viewModelScope.launch {
            _uiState.value = BodyPartUiState.Loading

            repository.addDefaultBodyPart("Face", "face")
                .onSuccess {
                    _uiState.value = BodyPartUiState.Success("Face added")
                }
                .onFailure { error ->
                    _uiState.value = BodyPartUiState.Error(
                        error.message ?: "Failed to add Face"
                    )
                }
        }
    }

    /**
     * Update an existing body part
     */
    fun updateBodyPart(bodyPart: BodyPart) {
        viewModelScope.launch {
            _uiState.value = BodyPartUiState.Loading

            repository.updateBodyPart(bodyPart)
                .onSuccess {
                    _uiState.value = BodyPartUiState.Success("Body part updated")
                }
                .onFailure { error ->
                    _uiState.value = BodyPartUiState.Error(
                        error.message ?: "Failed to update body part"
                    )
                }
        }
    }

    /**
     * Delete a body part
     */
    fun deleteBodyPart(bodyPart: BodyPart) {
        viewModelScope.launch {
            _uiState.value = BodyPartUiState.Loading

            repository.deleteBodyPart(bodyPart)
                .onSuccess {
                    _uiState.value = BodyPartUiState.Success("Body part deleted")
                }
                .onFailure { error ->
                    _uiState.value = BodyPartUiState.Error(
                        error.message ?: "Failed to delete body part"
                    )
                }
        }
    }

    /**
     * Toggle body part active status
     */
    fun toggleActive(bodyPart: BodyPart) {
        viewModelScope.launch {
            repository.toggleActive(bodyPart.id, !bodyPart.isActive)
                .onFailure { error ->
                    _uiState.value = BodyPartUiState.Error(
                        error.message ?: "Failed to toggle body part"
                    )
                }
        }
    }

    /**
     * Reorder body parts (e.g., after drag and drop)
     */
    fun reorderBodyParts(bodyParts: List<BodyPart>) {
        viewModelScope.launch {
            repository.reorderBodyParts(bodyParts)
                .onFailure { error ->
                    _uiState.value = BodyPartUiState.Error(
                        error.message ?: "Failed to reorder body parts"
                    )
                }
        }
    }

    /**
     * Clear UI state
     */
    fun clearUiState() {
        _uiState.value = BodyPartUiState.Idle
    }
}

/**
 * UI state for body part operations
 */
sealed class BodyPartUiState {
    object Idle : BodyPartUiState()
    object Loading : BodyPartUiState()
    data class Success(val message: String) : BodyPartUiState()
    data class Error(val message: String) : BodyPartUiState()
}
