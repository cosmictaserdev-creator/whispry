package com.example.whispry.domain.repository

import  com.example.whispry.domain.util.Result

interface AudioRepository {

    suspend fun transcribeAudio(
        audioFilePath: String,
        languageCode: String
    ): Result<String>
}
