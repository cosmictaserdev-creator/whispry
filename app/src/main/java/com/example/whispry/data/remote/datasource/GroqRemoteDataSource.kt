package com.example.whispry.data.remote.datasource

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.whispry.data.remote.api.GroqApiService
import com.example.whispry.data.remote.api.dto.TranscriptionResponseDto
import com.example.whispry.domain.util.Result
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class GroqRemoteDataSource @Inject constructor(
    private val apiService: GroqApiService,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {

    private val TAG = "Whispry_Groq"

    suspend fun transcribeAudio(
        apiKey: String,
        audioFilePath: String,
        languageCode: String
    ): Result<String> {
        Log.d(TAG, "transcribeAudio: keyPrefix=${apiKey.take(5)}..., file=$audioFilePath")

        if (!isNetworkAvailable(context)) {
            return Result.Error("no_internet")
        }

        return try {
            val audioFile = File(audioFilePath)
            if (!audioFile.exists()) {
                Log.e(TAG, "File does not exist: $audioFilePath")
                return Result.Error("File missing")
            }
            Log.d(TAG, "File size: ${audioFile.length()} bytes")

            // build the multipart file part
            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = audioFile.name,
                body = audioFile.asRequestBody("audio/mpeg".toMediaType())
            )

            val response = apiService.transcribeAudio(
                authorization = "Bearer $apiKey",
                file = filePart,
                model = MultipartBody.Part.createFormData("model", "whisper-large-v3"),
                language = MultipartBody.Part.createFormData("language", languageCode),
                responseFormat = MultipartBody.Part.createFormData("response_format", "json"),
                temperature = MultipartBody.Part.createFormData("temperature", "0.0")
            )

            if (response.isSuccessful) {
                val text = response.body()?.text
                if (text != null) {
                    Log.d(TAG, "Success! Transcribed ${text.length} chars")
                    Result.Success(text)
                } else {
                    Log.e(TAG, "Empty response body")
                    Result.Error("Empty response from Groq")
                }
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = parseErrorBody(errorJson)
                Log.e(TAG, "API Error (${response.code()}): $errorMessage | JSON: $errorJson")
                Result.Error(errorMessage, null)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}", e)
            Result.Error("Permission denied: ${e.message ?: "Network"}")
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "UnknownHostException: No internet")
            Result.Error("No internet connection")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            Result.Error("Error: ${e.message ?: "Unknown"}")
        }
    }

    private fun parseErrorBody(errorJson: String?): String {
        if (errorJson == null) return "Unknown error"
        return try {
            val errorDto = gson.fromJson(errorJson,
                com.example.whispry.data.remote.api.dto.ErrorResponseDto::class.java)
            errorDto.error?.message ?: "Unknown error"
        } catch (e: Exception) {
            "Unknown error"
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}