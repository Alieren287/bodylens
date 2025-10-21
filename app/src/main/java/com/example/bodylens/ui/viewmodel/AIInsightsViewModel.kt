package com.example.bodylens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodylens.ai.AIAnalysisEngine
import com.example.bodylens.data.model.AIInsight
import com.example.bodylens.data.model.InsightType
import com.example.bodylens.data.repository.ProgressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AIInsightsViewModel(
    private val repository: ProgressRepository,
    private val aiEngine: AIAnalysisEngine
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AIInsightsUiState>(AIInsightsUiState.Loading)
    val uiState: StateFlow<AIInsightsUiState> = _uiState.asStateFlow()
    
    private val _selectedInsightType = MutableStateFlow<InsightType?>(null)
    val selectedInsightType: StateFlow<InsightType?> = _selectedInsightType.asStateFlow()
    
    init {
        loadInsights()
    }
    
    private fun loadInsights() {
        viewModelScope.launch {
            repository.getRecentInsights(50)
                .catch { exception ->
                    _uiState.value = AIInsightsUiState.Error(exception.message ?: "Failed to load insights")
                }
                .collect { insights ->
                    _uiState.value = if (insights.isEmpty()) {
                        AIInsightsUiState.Empty
                    } else {
                        AIInsightsUiState.Success(insights)
                    }
                }
        }
    }
    
    fun generateInsightsForEntry(entryId: Long) {
        viewModelScope.launch {
            _uiState.value = AIInsightsUiState.Analyzing
            
            try {
                val entryWithDetails = repository.getEntryWithDetails(entryId)
                val allEntries = repository.getAllEntriesWithDetails().first()
                
                if (entryWithDetails != null) {
                    // Generate various insights
                    val progressInsights = aiEngine.analyzeProgress(allEntries)
                    val recommendations = aiEngine.generateRecommendations(entryWithDetails, allEntries)
                    val bodyComposition = aiEngine.analyzeBodyComposition(
                        entryWithDetails,
                        allEntries.getOrNull(1)
                    )
                    
                    val allInsights = progressInsights + recommendations + bodyComposition
                    
                    // Save insights to database
                    repository.saveInsights(allInsights)
                    
                    loadInsights()
                } else {
                    _uiState.value = AIInsightsUiState.Error("Entry not found")
                }
            } catch (e: Exception) {
                _uiState.value = AIInsightsUiState.Error(e.message ?: "Analysis failed")
            }
        }
    }
    
    fun filterByInsightType(type: InsightType?) {
        _selectedInsightType.value = type
        
        viewModelScope.launch {
            if (type != null) {
                repository.getInsightsByType(type)
                    .collect { insights ->
                        _uiState.value = AIInsightsUiState.Success(insights)
                    }
            } else {
                loadInsights()
            }
        }
    }
    
    fun refreshInsights() {
        loadInsights()
    }
    
    private suspend fun <T> Flow<T>.first(): T {
        var result: T? = null
        collect { value ->
            if (result == null) {
                result = value
            }
        }
        return result ?: throw NoSuchElementException("Flow was empty")
    }
}

sealed class AIInsightsUiState {
    data object Loading : AIInsightsUiState()
    data object Analyzing : AIInsightsUiState()
    data object Empty : AIInsightsUiState()
    data class Success(val insights: List<AIInsight>) : AIInsightsUiState()
    data class Error(val message: String) : AIInsightsUiState()
}



