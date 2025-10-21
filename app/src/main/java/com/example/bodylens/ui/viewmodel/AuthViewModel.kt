package com.example.bodylens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodylens.data.preferences.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        viewModelScope.launch {
            combine(
                userPreferences.isFirstLaunch,
                userPreferences.isPinSet,
                userPreferences.isPinEnabled
            ) { isFirstLaunch, isPinSet, isPinEnabled ->
                when {
                    isFirstLaunch -> AuthState.FirstLaunch
                    !isPinEnabled -> AuthState.Authenticated // PIN disabled, skip auth
                    !isPinSet -> AuthState.SetupRequired
                    else -> {
                        val shouldLock = userPreferences.shouldLock()
                        if (shouldLock) AuthState.Locked else AuthState.Authenticated
                    }
                }
            }.collect { state ->
                _authState.value = state
            }
        }
    }
    
    fun onPinDigitEntered(digit: String) {
        val currentPin = _pinInput.value
        if (currentPin.length < 4) {
            _pinInput.value = currentPin + digit
            
            if (_pinInput.value.length == 4) {
                handlePinComplete()
            }
        }
    }
    
    fun onPinBackspace() {
        val currentPin = _pinInput.value
        if (currentPin.isNotEmpty()) {
            _pinInput.value = currentPin.dropLast(1)
        }
    }
    
    fun clearPin() {
        _pinInput.value = ""
    }
    
    private fun handlePinComplete() {
        viewModelScope.launch {
            val enteredPin = _pinInput.value
            
            when (val currentState = _authState.value) {
                is AuthState.FirstLaunch,
                is AuthState.SetupRequired -> {
                    // First PIN entry - move to confirmation
                    _authState.value = AuthState.ConfirmPin(enteredPin)
                    clearPin()
                }
                
                is AuthState.ConfirmPin -> {
                    // Second PIN entry - verify match
                    if (enteredPin == currentState.originalPin) {
                        userPreferences.setPin(enteredPin)
                        userPreferences.setFirstLaunchComplete()
                        userPreferences.updateLastUnlockTime()
                        _authState.value = AuthState.Authenticated
                    } else {
                        _authState.value = AuthState.PinMismatch
                        clearPin()
                    }
                }
                
                is AuthState.Locked -> {
                    val isValid = userPreferences.verifyPin(enteredPin)
                    if (isValid) {
                        userPreferences.updateLastUnlockTime()
                        _authState.value = AuthState.Authenticated
                    } else {
                        _authState.value = AuthState.InvalidPin
                        clearPin()
                    }
                }
                
                else -> {}
            }
        }
    }
    
    fun resetPinMismatch() {
        _authState.value = AuthState.SetupRequired
    }
    
    fun resetInvalidPin() {
        _authState.value = AuthState.Locked
    }
    
    fun lockApp() {
        _authState.value = AuthState.Locked
        clearPin()
    }
    
    fun skipPinSetup() {
        viewModelScope.launch {
            userPreferences.skipPinSetup()
            userPreferences.setFirstLaunchComplete()
            _authState.value = AuthState.Authenticated
        }
    }
}

sealed class AuthState {
    data object Loading : AuthState()
    data object FirstLaunch : AuthState()
    data object SetupRequired : AuthState()
    data class ConfirmPin(val originalPin: String) : AuthState()
    data object PinMismatch : AuthState()
    data object Locked : AuthState()
    data object InvalidPin : AuthState()
    data object Authenticated : AuthState()
}



