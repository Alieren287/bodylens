package com.progresstracker.bodylens.auth.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for PIN using EncryptedSharedPreferences
 */
class SecurePinStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Save PIN securely
     */
    fun savePin(pin: String) {
        sharedPreferences.edit()
            .putString(KEY_PIN, pin)
            .apply()
    }

    /**
     * Get saved PIN
     */
    fun getPin(): String? {
        return sharedPreferences.getString(KEY_PIN, null)
    }

    /**
     * Check if PIN exists
     */
    fun isPinSet(): Boolean {
        return getPin() != null
    }

    /**
     * Verify if provided PIN matches saved PIN
     */
    fun verifyPin(pin: String): Boolean {
        return getPin() == pin
    }

    /**
     * Clear PIN (for reset functionality)
     */
    fun clearPin() {
        sharedPreferences.edit()
            .remove(KEY_PIN)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "bodylens_secure_prefs"
        private const val KEY_PIN = "user_pin"
    }
}
