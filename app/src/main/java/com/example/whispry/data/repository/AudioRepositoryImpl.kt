package com.example.whispry.data.repository

import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.remote.datasource.GroqRemoteDataSource
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.util.Result
import javax.inject.Inject

class AudioRepositoryImpl @Inject constructor(
    private val remoteDataSource: GroqRemoteDataSource,
    private val apiKeyProvider: ApiKeyProvider
) : AudioRepository {

    override suspend fun transcribeAudio(
        audioFilePath: String,
        languageCode: String
    ): Result<String> {

        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isBlank()) {
            return Result.Error("Groq API key not set. Please add it in Settings.")
        }

        return remoteDataSource.transcribeAudio(
            apiKey = apiKey,
            audioFilePath = audioFilePath,
            languageCode = languageCode
        )
    }
}