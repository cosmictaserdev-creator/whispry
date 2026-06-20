package com.example.whispry.data.repository

import com.example.whispry.data.local.datasource.ApiKeyProvider
import com.example.whispry.data.remote.api.GroqChatApiService
import com.example.whispry.data.remote.api.dto.ChatCompletionRequest
import com.example.whispry.data.remote.api.dto.ChatMessage
import com.example.whispry.domain.repository.GroqFormatterRepository
import com.example.whispry.domain.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqFormatterRepositoryImpl @Inject constructor(
    private val groqChatApiService: GroqChatApiService,
    private val apiKeyProvider: ApiKeyProvider
) : GroqFormatterRepository {

    override suspend fun formatText(
        rawText: String,
        systemPrompt: String
    ): Result<String> {
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isBlank()) return Result.Error("API key not set")

        return try {
            val response = groqChatApiService.chatCompletion(
                authorization = "Bearer $apiKey",
                request = ChatCompletionRequest(
                    model = "llama-3.3-70b-versatile",  // superior reasoning for intelligent formatting
                    messages = listOf(
                        ChatMessage(
                            role = "system",
                            content = systemPrompt
                        ),
                        ChatMessage(
                            role = "user",
                            content = rawText
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
            // formatting failed — return original text as fallback
            Result.Success(rawText) 
        }
    }
}
