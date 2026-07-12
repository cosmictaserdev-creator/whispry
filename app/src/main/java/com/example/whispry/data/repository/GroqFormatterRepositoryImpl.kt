package com.example.whispry.data.repository

import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.data.remote.api.GroqChatApiService
import com.example.whispry.data.remote.api.dto.ChatCompletionRequest
import com.example.whispry.data.remote.api.dto.ChatMessage
import com.example.whispry.domain.model.FormattingProviderPreset
import com.example.whispry.domain.model.ProviderConfigResolver
import com.example.whispry.domain.repository.GroqFormatterRepository
import com.example.whispry.domain.util.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqFormatterRepositoryImpl @Inject constructor(
    private val groqChatApiService: GroqChatApiService,
    private val apiKeyProvider: ApiKeyProvider,
    private val settingsProvider: SettingsProvider
) : GroqFormatterRepository {

    override suspend fun formatText(
        userContent: String,
        systemPrompt: String,
        fallbackText: String
    ): Result<String> {
        val prefs = settingsProvider.dataStore.data.first()
        val preset = FormattingProviderPreset.fromName(prefs[DataStoreKeys.FORMATTING_PROVIDER_PRESET])
        val resolved = ProviderConfigResolver.resolveFormatting(
            preset = preset,
            customBaseUrl = prefs[DataStoreKeys.FORMATTING_CUSTOM_BASE_URL] ?: "",
            customModel = prefs[DataStoreKeys.FORMATTING_CUSTOM_MODEL] ?: "",
            apiKey = apiKeyProvider.getFormattingApiKey(preset)
        )
        if (resolved.apiKey.isBlank()) return Result.Error("API key not set")
        if (resolved.baseUrl.isBlank()) return Result.Success(fallbackText)

        return try {
            val response = groqChatApiService.chatCompletion(
                url = resolved.baseUrl + "chat/completions",
                authorization = "Bearer ${resolved.apiKey}",
                request = ChatCompletionRequest(
                    model = resolved.model,
                    messages = listOf(
                        ChatMessage(
                            role = "system",
                            content = systemPrompt
                        ),
                        ChatMessage(
                            role = "user",
                            content = userContent
                        )
                    ),
                    maxTokens = 1024,
                    temperature = 0.3f  // low temperature = consistent formatting
                )
            )

            if (response.isSuccessful) {
                val formattedText = response.body()
                    ?.choices
                    ?.firstOrNull()
                    ?.message
                    ?.content
                    ?.trim()

                if (formattedText != null) {
                    Result.Success(formattedText)
                } else {
                    Result.Error("Empty response from formatter")
                }
            } else {
                Result.Error("Formatting failed: ${response.code()}")
            }
        } catch (e: Exception) {
            // formatting failed — return the clean fallback text (never the wrapped content)
            Result.Success(fallbackText)
        }
    }
}
