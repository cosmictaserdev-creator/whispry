package com.example.whispry.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class TranscriptionResponseDto(
    @SerializedName("text")
    val text: String
)