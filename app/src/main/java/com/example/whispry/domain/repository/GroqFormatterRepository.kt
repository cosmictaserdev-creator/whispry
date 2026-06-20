package com.example.whispry.domain.repository

import com.example.whispry.domain.util.Result

interface GroqFormatterRepository {
    suspend fun formatText(
        rawText: String,
        systemPrompt: String
    ): Result<String>
}
