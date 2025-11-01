package com.progresstracker.bodylens.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.progresstracker.bodylens.data.dao.BodyPartDao
import com.progresstracker.bodylens.data.dao.PhotoDao
import com.progresstracker.bodylens.data.dao.SessionDao
import com.progresstracker.bodylens.data.entity.BodyPart
import com.progresstracker.bodylens.data.entity.DefaultBodyParts
import com.progresstracker.bodylens.data.entity.Photo
import com.progresstracker.bodylens.data.entity.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main database for BodyLens app
 */
@Database(
    entities = [
        BodyPart::class,
        Session::class,
        Photo::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bodyPartDao(): BodyPartDao
    abstract fun sessionDao(): SessionDao
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bodylens_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Callback to populate database with default data on first creation
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.bodyPartDao())
                    }
                }
            }
        }

        /**
         * Populate database with default body parts
         */
        private suspend fun populateDatabase(bodyPartDao: BodyPartDao) {
            // Insert default body parts if database is empty
            val count = bodyPartDao.getCount()
            if (count == 0) {
                bodyPartDao.insertAll(DefaultBodyParts.defaults)
            }
        }
    }
}
