package com.example.whispry.data.remote.api

import com.example.whispry.data.remote.api.dto.TranscriptionResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface GroqApiService {

    @Multipart
    @POST("openai/v1/audio/transcriptions")
    suspend fun transcribeAudio(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Part model: MultipartBody.Part,
        @Part language: MultipartBody.Part,
        @Part responseFormat: MultipartBody.Part,
        @Part temperature: MultipartBody.Part
    ): Response<TranscriptionResponseDto>
}