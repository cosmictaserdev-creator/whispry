package com.example.whispry.data.remote.api

import com.example.whispry.data.remote.api.dto.ChatCompletionRequest
import com.example.whispry.data.remote.api.dto.ChatCompletionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Despite the name (kept to avoid a repo-wide rename), this now targets any OpenAI-compatible
 * chat-completions endpoint — the full URL is resolved per user-selected provider and passed in,
 * rather than fixed to Groq's base URL.
 */
interface GroqChatApiService {

    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>

    /**
     * GET .../models — every OpenAI-compatible provider exposes this. Used to test whether an
     * API key is valid without spending tokens or needing a model that fits the calling step
     * (a transcription step's model, e.g. "whisper-large-v3", isn't a chat model, so testing
     * via [chatCompletion] would reject a perfectly valid key).
     */
    @GET
    suspend fun listModels(
        @Url url: String,
        @Header("Authorization") authorization: String
    ): Response<Any>
}
