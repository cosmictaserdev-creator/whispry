// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.repository

import com.example.whispry.domain.model.Transcript
import com.example.whispry.domain.model.TranscriptStats
import kotlinx.coroutines.flow.Flow

interface TranscriptRepository {

    fun getAllTranscripts(): Flow<List<Transcript>>

    fun getRecentTranscripts(limit: Int): Flow<List<Transcript>>

    fun getStats(): Flow<TranscriptStats>

    suspend fun saveTranscript(
        text: String,
        rawText: String,
        durationMs: Long,
        languageCode: String,
        preset: String
    )

    suspend fun deleteTranscript(id: Long)

    suspend fun clearAll()

    suspend fun deleteAll()

    suspend fun deleteTranscriptsOlderThan(thresholdMs: Long)

    suspend fun updatePinStatus(id: Long, isPinned: Boolean)

    suspend fun updateTranscript(transcript: Transcript)

    suspend fun getTranscriptById(id: Long): Transcript?
}