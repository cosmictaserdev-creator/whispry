package com.example.whispry.data.repository

import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.data.remote.datasource.GroqRemoteDataSource
import com.example.whispry.domain.model.ProviderConfigResolver
import com.example.whispry.domain.model.TranscriptionProviderPreset
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.util.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AudioRepositoryImpl @Inject constructor(
    private val remoteDataSource: GroqRemoteDataSource,
    private val apiKeyProvider: ApiKeyProvider,
    private val settingsProvider: SettingsProvider
) : AudioRepository {

    override suspend fun transcribeAudio(
        audioFilePath: String,
        languageCode: String
    ): Result<String> {
        val prefs = settingsProvider.dataStore.data.first()
        val preset = TranscriptionProviderPreset.fromName(prefs[DataStoreKeys.TRANSCRIPTION_PROVIDER_PRESET])
        val resolved = ProviderConfigResolver.resolveTranscription(
            preset = preset,
            customBaseUrl = prefs[DataStoreKeys.TRANSCRIPTION_CUSTOM_BASE_URL] ?: "",
            customModel = prefs[DataStoreKeys.TRANSCRIPTION_CUSTOM_MODEL] ?: "",
            apiKey = apiKeyProvider.getTranscriptionApiKey(preset)
        )
        if (resolved.apiKey.isBlank()) {
            return Result.Error("API key not set. Please add it in Settings.")
        }
        if (resolved.baseUrl.isBlank()) {
            return Result.Error("Transcription endpoint not set. Please add a Custom base URL in Settings.")
        }

        return remoteDataSource.transcribeAudio(
            apiKey = resolved.apiKey,
            audioFilePath = audioFilePath,
            languageCode = languageCode,
            baseUrl = resolved.baseUrl,
            model = resolved.model
        )
    }
}
