// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.di

import android.content.Context
import android.media.AudioManager
import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.TranscriptLocalDataSource
import com.example.whispry.data.local.datasource.UsageDataStore
import com.example.whispry.data.repository.UsageRepositoryImpl
import com.example.whispry.domain.repository.UsageRepository
import com.example.whispry.data.remote.datasource.GroqRemoteDataSource
import com.example.whispry.data.repository.AudioRepositoryImpl
import com.example.whispry.data.repository.GroqFormatterRepositoryImpl
import com.example.whispry.data.repository.TranscriptRepositoryImpl
import com.example.whispry.data.repository.TriggerRepositoryImpl
import com.example.whispry.domain.repository.MemoryRepository
import com.example.whispry.data.repository.MemoryRepositoryImpl
import com.example.whispry.data.local.db.MemoryDao
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.repository.GroqFormatterRepository
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.repository.TriggerRepository
import com.example.whispry.features.expander.data.local.db.TextExpanderDao
import com.example.whispry.features.expander.data.repository.TextExpanderRepositoryImpl
import com.example.whispry.features.expander.domain.repository.TextExpanderRepository
import com.example.whispry.features.tone.data.local.db.AppToneDao
import com.example.whispry.features.tone.data.repository.AppToneRepositoryImpl
import com.example.whispry.features.tone.domain.repository.AppToneRepository
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
    fun provideMemoryRepository(
        dao: MemoryDao
    ): MemoryRepository {
        return MemoryRepositoryImpl(dao)
    }

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
        apiKeyProvider: ApiKeyProvider,
        settingsProvider: com.example.whispry.data.local.datasource.SettingsProvider
    ): AudioRepository {
        return AudioRepositoryImpl(remoteDataSource, apiKeyProvider, settingsProvider)
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
    fun provideGroqFormatterRepository(
        apiService: com.example.whispry.data.remote.api.GroqChatApiService,
        apiKeyProvider: ApiKeyProvider,
        settingsProvider: com.example.whispry.data.local.datasource.SettingsProvider
    ): GroqFormatterRepository {
        return GroqFormatterRepositoryImpl(apiService, apiKeyProvider, settingsProvider)
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

    @Provides
    @Singleton
    fun provideTextExpanderRepository(
        dao: TextExpanderDao
    ): TextExpanderRepository {
        return TextExpanderRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideAppToneRepository(
        dao: AppToneDao
    ): AppToneRepository {
        return AppToneRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideMyInfoRepository(
        dao: com.example.whispry.features.myinfo.data.local.db.MyInfoDao
    ): com.example.whispry.features.myinfo.domain.repository.MyInfoRepository {
        return com.example.whispry.features.myinfo.data.repository.MyInfoRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideVoiceCommandRepository(
        dao: com.example.whispry.features.voicecommand.data.local.db.VoiceCommandDao
    ): com.example.whispry.features.voicecommand.domain.repository.VoiceCommandRepository {
        return com.example.whispry.features.voicecommand.data.repository.VoiceCommandRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideUsageRepository(
        usageDataStore: UsageDataStore
    ): UsageRepository {
        return UsageRepositoryImpl(usageDataStore)
    }

    @Provides
    @Singleton
    fun provideWhispryNotificationManager(
        @ApplicationContext context: Context,
        settingsProvider: com.example.whispry.data.local.datasource.SettingsProvider
    ): com.example.whispry.notification.WhispryNotificationManager {
        return com.example.whispry.notification.WhispryNotificationManager(context, settingsProvider)
    }
}