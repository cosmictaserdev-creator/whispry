package com.example.whispry.data.remote.api

import com.example.whispry.data.remote.api.dto.TranscriptionResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

/**
 * Despite the name (kept to avoid a repo-wide rename), this now targets any OpenAI-compatible
 * transcription endpoint — the full URL is resolved per user-selected provider and passed in,
 * rather than fixed to Groq's base URL.
 */
interface GroqApiService {

    @Multipart
    @POST
    suspend fun transcribeAudio(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Part model: MultipartBody.Part,
        @Part language: MultipartBody.Part,
        @Part responseFormat: MultipartBody.Part,
        @Part temperature: MultipartBody.Part
    ): Response<TranscriptionResponseDto>
}
