package com.example.whispry.data.local.db


import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TranscriptEntity::class],
    version = 3,
    exportSchema = true          // exports schema to a JSON file for migration tracking
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transcriptDao(): TranscriptDao

    companion object {
        const val DATABASE_NAME = "whispry_database"
    }
}