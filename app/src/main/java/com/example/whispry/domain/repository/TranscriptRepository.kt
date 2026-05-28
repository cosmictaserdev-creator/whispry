package com.example.whispry.domain.repository

import com.example.whispry.domain.model.Transcript
import kotlinx.coroutines.flow.Flow

interface TranscriptRepository {

    fun getAllTranscripts(): Flow<List<Transcript>>

    suspend fun saveTranscript(text: String, durationMs: Long, languageCode: String)

    suspend fun deleteTranscript(id: Long)

    suspend fun clearAll()

    suspend fun updatePinStatus(id: Long, isPinned: Boolean)

    suspend fun getTranscriptById(id: Long): Transcript?
}