package com.example.whispry.di

import android.content.Context
import androidx.room.Room
import com.example.whispry.data.local.db.AppDatabase
import com.example.whispry.data.local.db.TranscriptDao
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
            .fallbackToDestructiveMigration() // during development only
            .build()
    }

    @Provides
    @Singleton
    fun provideTranscriptDao(database: AppDatabase): TranscriptDao {
        return database.transcriptDao()
    }
}