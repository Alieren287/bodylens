package com.example.bodylens

import android.app.Application
import com.example.bodylens.ai.AIAnalysisEngine
import com.example.bodylens.data.local.BodyLensDatabase
import com.example.bodylens.data.preferences.UserPreferences
import com.example.bodylens.data.repository.ProgressRepository
import com.example.bodylens.data.security.EncryptedFileManager

class BodyLensApp : Application() {
    
    private var _database: BodyLensDatabase? = null
    val database: BodyLensDatabase?
        get() = _database
    
    lateinit var userPreferences: UserPreferences
    lateinit var encryptedFileManager: EncryptedFileManager
    lateinit var aiEngine: AIAnalysisEngine
    
    private var _repository: ProgressRepository? = null
    val repository: ProgressRepository?
        get() = _repository
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize non-database dependencies
        userPreferences = UserPreferences(this)
        encryptedFileManager = EncryptedFileManager(this)
        aiEngine = AIAnalysisEngine()
    }
    
    fun initializeDatabase(passphrase: String) {
        if (_database == null) {
            _database = BodyLensDatabase.getInstance(this, passphrase)
            _repository = ProgressRepository(_database!!, encryptedFileManager)
        }
    }
    
    fun closeDatabase() {
        _database?.close()
        _database = null
        _repository = null
    }
    
    companion object {
        lateinit var instance: BodyLensApp
            private set
    }
}




