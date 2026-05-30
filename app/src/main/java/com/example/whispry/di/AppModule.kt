package com.example.whispry.di

import android.content.Context
import android.media.AudioManager
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.TranscriptLocalDataSource
import com.example.whispry.data.remote.datasource.GroqRemoteDataSource
import com.example.whispry.data.repository.AudioRepositoryImpl
import com.example.whispry.data.repository.TranscriptRepositoryImpl
import com.example.whispry.data.repository.TriggerRepositoryImpl
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.repository.TriggerRepository
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTranscriptRepository(
        localDataSource: TranscriptLocalDataSource
    ): TranscriptRepository {
        return TranscriptRepositoryImpl(localDataSource)
    }

    @Provides
    @Singleton
    fun provideAudioRepository(
        remoteDataSource: GroqRemoteDataSource,
        apiKeyProvider: ApiKeyProvider
    ): AudioRepository {
        return AudioRepositoryImpl(remoteDataSource, apiKeyProvider)
    }

    @Provides
    @Singleton
    fun provideTriggerRepository(
        settingsProvider: com.example.whispry.data.local.datasource.SettingsProvider
    ): TriggerRepository {
        return TriggerRepositoryImpl(settingsProvider)
    }

    @Provides
    @Singleton
    fun provideGroqRemoteDataSource(
        apiService: com.example.whispry.data.remote.api.GroqApiService,
        gson: Gson,
        @ApplicationContext context: Context
    ): GroqRemoteDataSource {
        return GroqRemoteDataSource(apiService, gson, context)
    }

    @Provides
    @Singleton
    fun provideTranscriptLocalDataSource(
        dao: com.example.whispry.data.local.db.TranscriptDao
    ): TranscriptLocalDataSource {
        return TranscriptLocalDataSource(dao)
    }

    @Provides
    @Singleton
    fun provideAudioManager(
        @ApplicationContext context: Context
    ): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}