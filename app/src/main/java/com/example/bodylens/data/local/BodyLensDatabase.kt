package com.example.bodylens.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.bodylens.data.local.dao.*
import com.example.bodylens.data.model.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        ProgressEntry::class,
        ProgressPhoto::class,
        Measurement::class,
        AIInsight::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BodyLensDatabase : RoomDatabase() {
    
    abstract fun progressEntryDao(): ProgressEntryDao
    abstract fun progressPhotoDao(): ProgressPhotoDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun aiInsightDao(): AIInsightDao
    
    companion object {
        @Volatile
        private var INSTANCE: BodyLensDatabase? = null
        
        private const val DATABASE_NAME = "bodylens_database"
        
        fun getInstance(context: Context, passphrase: String): BodyLensDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context, passphrase)
                INSTANCE = instance
                instance
            }
        }
        
        private fun buildDatabase(context: Context, passphrase: String): BodyLensDatabase {
            // Create encrypted database using SQLCipher
            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
            
            return Room.databaseBuilder(
                context.applicationContext,
                BodyLensDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
        
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}


