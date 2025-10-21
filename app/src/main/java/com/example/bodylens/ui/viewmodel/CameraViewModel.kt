package com.example.bodylens.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodylens.data.model.PhotoGroup
import com.example.bodylens.data.preferences.UserPreferences
import com.example.bodylens.data.repository.ProgressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CameraViewModel(
    private val repository: ProgressRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    
    private val _selectedPhotoGroup = MutableStateFlow(PhotoGroup.FACE)
    val selectedPhotoGroup: StateFlow<PhotoGroup> = _selectedPhotoGroup.asStateFlow()
    
    private val _capturedPhotos = MutableStateFlow<Map<PhotoGroup, Boolean>>(emptyMap())
    val capturedPhotos: StateFlow<Map<PhotoGroup, Boolean>> = _capturedPhotos.asStateFlow()
    
    private val _isFrontCamera = MutableStateFlow(true) // Start with front camera for face photos
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()
    
    // Get enabled photo groups from preferences
    val enabledPhotoGroups: StateFlow<List<PhotoGroup>> = userPreferences.enabledPhotoGroups
        .map { enabledSet ->
            PhotoGroup.entries.filter { it.name in enabledSet }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(PhotoGroup.FACE, PhotoGroup.FRONT, PhotoGroup.BACK, PhotoGroup.SIDE_LEFT, PhotoGroup.SIDE_RIGHT)
        )
    
    init {
        // Set initial selected photo group to first enabled group
        viewModelScope.launch {
            enabledPhotoGroups.collect { groups ->
                if (groups.isNotEmpty() && !groups.contains(_selectedPhotoGroup.value)) {
                    _selectedPhotoGroup.value = groups.first()
                }
            }
        }
    }
    
    fun selectPhotoGroup(group: PhotoGroup) {
        _selectedPhotoGroup.value = group
        // Smart auto-switching: suggest front camera for face photos, back for body photos
        if (group == PhotoGroup.FACE && !_isFrontCamera.value) {
            _isFrontCamera.value = true
        } else if (group != PhotoGroup.FACE && _isFrontCamera.value) {
            _isFrontCamera.value = false
        }
    }
    
    fun toggleCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
    }
    
    fun capturePhoto(entryId: Long, bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Saving
            
            try {
                // Save photo on IO thread for non-blocking operation
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.savePhoto(
                        entryId = entryId,
                        photoGroup = _selectedPhotoGroup.value,
                        bitmap = bitmap
                    )
                }
                
                // Mark this photo group as captured (on main thread)
                val updated = _capturedPhotos.value.toMutableMap()
                updated[_selectedPhotoGroup.value] = true
                _capturedPhotos.value = updated
                
                _uiState.value = CameraUiState.PhotoSaved(_selectedPhotoGroup.value)
                
                // Auto-select next photo group if not all captured
                selectNextPhotoGroup()
                
            } catch (e: Exception) {
                _uiState.value = CameraUiState.Error(e.message ?: "Failed to save photo")
            }
        }
    }
    
    private fun selectNextPhotoGroup() {
        val allGroups = enabledPhotoGroups.value
        
        val currentIndex = allGroups.indexOf(_selectedPhotoGroup.value)
        if (currentIndex < allGroups.size - 1) {
            _selectedPhotoGroup.value = allGroups[currentIndex + 1]
        }
    }
    
    fun resetState() {
        _uiState.value = CameraUiState.Idle
    }
    
    fun areAllPhotosCaptured(): Boolean {
        val requiredGroups = enabledPhotoGroups.value
        return requiredGroups.isNotEmpty() && requiredGroups.all { _capturedPhotos.value[it] == true }
    }
    
    fun hasAnyPhotoCaptured(): Boolean {
        return _capturedPhotos.value.any { it.value }
    }
    
    fun resetCapturedPhotos() {
        _capturedPhotos.value = emptyMap()
    }
}

sealed class CameraUiState {
    data object Idle : CameraUiState()
    data object Saving : CameraUiState()
    data class PhotoSaved(val group: PhotoGroup) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}



