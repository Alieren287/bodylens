package com.example.bodylens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodylens.data.model.ProgressEntry
import com.example.bodylens.data.model.ProgressEntryWithDetails
import com.example.bodylens.data.repository.ProgressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ProgressRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // Separate flow for navigation events
    private val _navigationEvent = MutableStateFlow<Long?>(null)
    val navigationEvent: StateFlow<Long?> = _navigationEvent.asStateFlow()
    
    init {
        loadProgressEntries()
    }
    
    private fun loadProgressEntries() {
        viewModelScope.launch {
            repository.getAllEntriesWithDetails()
                .catch { exception ->
                    // Only update if not in navigation state
                    if (_uiState.value !is HomeUiState.NavigateToCamera) {
                        _uiState.value = HomeUiState.Error(exception.message ?: "Unknown error")
                    }
                }
                .collect { entries ->
                    // Only update if not in navigation state
                    if (_uiState.value !is HomeUiState.NavigateToCamera) {
                        _uiState.value = if (entries.isEmpty()) {
                            HomeUiState.Empty
                        } else {
                            HomeUiState.Success(entries)
                        }
                    }
                }
        }
    }
    
    fun createNewEntry() {
        // Prevent double-clicks
        if (_uiState.value is HomeUiState.NavigateToCamera) return
        
        // Create entry and navigate immediately
        viewModelScope.launch {
            try {
                // Create entry on IO thread
                val entry = ProgressEntry()
                val entryId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.createEntry(entry)
                }
                
                // Set navigation state
                _uiState.value = HomeUiState.NavigateToCamera(entryId)
                _navigationEvent.value = entryId
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Failed to create entry: ${e.message}")
            }
        }
    }
    
    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
        }
    }
    
    fun getStorageInfo(): Flow<String> = flow {
        val storageBytes = repository.getTotalStorageUsed()
        val storageMB = storageBytes / (1024.0 * 1024.0)
        emit(String.format("%.2f MB used", storageMB))
    }
    
    fun resetNavigationState() {
        // Reset navigation state and reload entries
        _navigationEvent.value = null
        if (_uiState.value is HomeUiState.NavigateToCamera) {
            _uiState.value = HomeUiState.Loading
            loadProgressEntries()
        }
    }
    
    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data object Empty : HomeUiState()
    data class Success(val entries: List<ProgressEntryWithDetails>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    data class NavigateToCamera(val entryId: Long) : HomeUiState()
}



