package com.example.whispry.data.remote.api

import com.example.whispry.data.remote.api.dto.ChatCompletionRequest
import com.example.whispry.data.remote.api.dto.ChatCompletionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqChatApiService {

    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}
