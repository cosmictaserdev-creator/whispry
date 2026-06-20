package com.example.whispry.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.whispry.data.local.db.AppDatabase
import com.example.whispry.data.local.db.MemoryDao
import com.example.whispry.data.local.db.TranscriptDao
import com.example.whispry.features.expander.data.local.db.TextExpanderDao
import com.example.whispry.features.tone.data.local.db.AppToneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .addMigrations(
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
            .build()
     }

    @Provides
     @Singleton
     fun provideTranscriptDao(database: AppDatabase): TranscriptDao {
         return database.transcriptDao()
     }

     @Provides
     @Singleton
     fun provideTextExpanderDao(database: AppDatabase): TextExpanderDao {
         return database.textExpanderDao()
     }

    @Provides
    @Singleton
    fun provideAppToneDao(database: AppDatabase): AppToneDao {
        return database.appToneDao()
    }

    @Provides
    @Singleton
    fun provideMemoryDao(database: AppDatabase): MemoryDao {
        return database.memoryDao()
    }
}