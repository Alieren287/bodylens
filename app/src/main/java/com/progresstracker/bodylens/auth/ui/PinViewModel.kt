package com.progresstracker.bodylens.auth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progresstracker.bodylens.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for PIN authentication screens
 */
class PinViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val _pinState = MutableStateFlow<PinState>(PinState.Idle)
    val pinState: StateFlow<PinState> = _pinState.asStateFlow()

    private val _currentPin = MutableStateFlow("")
    val currentPin: StateFlow<String> = _currentPin.asStateFlow()

    /**
     * Add digit to current PIN
     */
    fun addDigit(digit: String) {
        if (_currentPin.value.length < AuthRepository.PIN_LENGTH) {
            _currentPin.value += digit

            // Auto-submit when PIN is complete
            if (_currentPin.value.length == AuthRepository.PIN_LENGTH) {
                // Small delay for better UX
                viewModelScope.launch {
                    kotlinx.coroutines.delay(100)
                    // Will be handled by the screen
                }
            }
        }
    }

    /**
     * Remove last digit
     */
    fun removeDigit() {
        if (_currentPin.value.isNotEmpty()) {
            _currentPin.value = _currentPin.value.dropLast(1)
        }
    }

    /**
     * Clear PIN input
     */
    fun clearPin() {
        _currentPin.value = ""
        _pinState.value = PinState.Idle
    }

    /**
     * Setup new PIN (first-time setup)
     */
    fun setupPin(pin: String, confirmPin: String) {
        viewModelScope.launch {
            _pinState.value = PinState.Loading

            if (pin != confirmPin) {
                _pinState.value = PinState.Error("PINs do not match")
                return@launch
            }

            authRepository.setupPin(pin)
                .onSuccess {
                    _pinState.value = PinState.Success
                }
                .onFailure { error ->
                    _pinState.value = PinState.Error(error.message ?: "Failed to setup PIN")
                }
        }
    }

    /**
     * Verify PIN
     */
    fun verifyPin(pin: String) {
        viewModelScope.launch {
            _pinState.value = PinState.Loading

            val isValid = authRepository.verifyPin(pin)
            if (isValid) {
                _pinState.value = PinState.Success
            } else {
                _pinState.value = PinState.Error("Incorrect PIN")
                // Clear after short delay
                kotlinx.coroutines.delay(500)
                clearPin()
            }
        }
    }

    /**
     * Check if PIN is already configured
     */
    fun isPinConfigured(): Boolean {
        return authRepository.isPinConfigured()
    }
}

/**
 * UI state for PIN screens
 */
sealed class PinState {
    object Idle : PinState()
    object Loading : PinState()
    object Success : PinState()
    data class Error(val message: String) : PinState()
}
