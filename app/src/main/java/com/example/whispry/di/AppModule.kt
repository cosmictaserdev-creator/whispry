package com.example.whispry.di

import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.TranscriptLocalDataSource
import com.example.whispry.data.remote.datasource.GroqRemoteDataSource
import com.example.whispry.data.repository.AudioRepositoryImpl
import com.example.whispry.data.repository.TranscriptRepositoryImpl
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.repository.TranscriptRepository
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideGroqRemoteDataSource(
        apiService: com.example.whispry.data.remote.api.GroqApiService,
        gson: Gson
    ): GroqRemoteDataSource {
        return GroqRemoteDataSource(apiService, gson)
    }

    @Provides
    @Singleton
    fun provideTranscriptLocalDataSource(
        dao: com.example.whispry.data.local.db.TranscriptDao
    ): TranscriptLocalDataSource {
        return TranscriptLocalDataSource(dao)
    }
}