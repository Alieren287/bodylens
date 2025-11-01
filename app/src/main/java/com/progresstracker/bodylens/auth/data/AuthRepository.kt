package com.progresstracker.bodylens.auth.data

import android.content.Context

/**
 * Repository for authentication operations
 */
class AuthRepository(context: Context) {

    private val pinStorage = SecurePinStorage(context)

    /**
     * Check if user has set up a PIN
     */
    fun isPinConfigured(): Boolean {
        return pinStorage.isPinSet()
    }

    /**
     * Setup new PIN (for first-time users)
     */
    fun setupPin(pin: String): Result<Unit> {
        return try {
            if (pin.length != PIN_LENGTH) {
                Result.failure(IllegalArgumentException("PIN must be $PIN_LENGTH digits"))
            } else if (!pin.all { it.isDigit() }) {
                Result.failure(IllegalArgumentException("PIN must contain only digits"))
            } else {
                pinStorage.savePin(pin)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify PIN
     */
    fun verifyPin(pin: String): Boolean {
        return pinStorage.verifyPin(pin)
    }

    /**
     * Clear PIN (for reset)
     */
    fun clearPin() {
        pinStorage.clearPin()
    }

    companion object {
        const val PIN_LENGTH = 4
    }
}
