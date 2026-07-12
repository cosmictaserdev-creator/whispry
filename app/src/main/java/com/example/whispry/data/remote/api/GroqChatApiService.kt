package com.example.whispry.data.remote.api

import com.example.whispry.data.remote.api.dto.ChatCompletionRequest
import com.example.whispry.data.remote.api.dto.ChatCompletionResponse
import retrofit2.Response
import retrofit2.http.Body
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
}
