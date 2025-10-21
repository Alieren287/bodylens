package com.example.bodylens.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodylens.data.model.*
import com.example.bodylens.data.repository.ProgressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val repository: ProgressRepository
) : ViewModel() {
    
    private val _selectedEntry = MutableStateFlow<ProgressEntryWithDetails?>(null)
    val selectedEntry: StateFlow<ProgressEntryWithDetails?> = _selectedEntry.asStateFlow()
    
    private val _photoImages = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())
    val photoImages: StateFlow<Map<Long, Bitmap>> = _photoImages.asStateFlow()
    
    private val _comparisonMode = MutableStateFlow(false)
    val comparisonMode: StateFlow<Boolean> = _comparisonMode.asStateFlow()
    
    private val _selectedPhotoGroup = MutableStateFlow(PhotoGroup.FACE)
    val selectedPhotoGroup: StateFlow<PhotoGroup> = _selectedPhotoGroup.asStateFlow()
    
    fun loadEntry(entryId: Long) {
        viewModelScope.launch {
            val entry = repository.getEntryWithDetails(entryId)
            _selectedEntry.value = entry
            
            // Load photo images
            entry?.photos?.forEach { photo ->
                loadPhotoImage(photo)
            }
        }
    }
    
    private fun loadPhotoImage(photo: ProgressPhoto) {
        viewModelScope.launch {
            val bitmap = repository.loadPhotoImage(photo)
            bitmap?.let {
                val updated = _photoImages.value.toMutableMap()
                updated[photo.id] = it
                _photoImages.value = updated
            }
        }
    }
    
    fun toggleComparisonMode() {
        _comparisonMode.value = !_comparisonMode.value
    }
    
    fun selectPhotoGroup(group: PhotoGroup) {
        _selectedPhotoGroup.value = group
    }
    
    fun addNote(entryId: Long, note: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            entry?.let {
                val updated = it.copy(notes = note)
                repository.updateEntry(updated)
                loadEntry(entryId)
            }
        }
    }
    
    fun addMeasurements(entryId: Long, measurements: List<Measurement>) {
        viewModelScope.launch {
            repository.saveMeasurements(measurements)
            loadEntry(entryId)
        }
    }
}



