package com.hartagis.edgear.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SessionEntity::class,
        MemoryEntity::class,
        TodoEntity::class,
        NoteEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class HermesDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun memoryDao(): MemoryDao
    abstract fun todoDao(): TodoDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: HermesDatabase? = null

        fun getDatabase(context: Context): HermesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HermesDatabase::class.java,
                    "hermes_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
