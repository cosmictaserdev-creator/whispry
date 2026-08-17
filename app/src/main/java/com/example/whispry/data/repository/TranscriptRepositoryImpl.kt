// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.data.repository

import com.example.whispry.data.local.datasource.TranscriptLocalDataSource
import com.example.whispry.data.local.db.TranscriptEntity
import com.example.whispry.data.local.db.toDomain
import com.example.whispry.data.local.db.toEntity
import com.example.whispry.domain.model.Transcript
import com.example.whispry.domain.model.TranscriptStats
import com.example.whispry.domain.repository.TranscriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TranscriptRepositoryImpl @Inject constructor(
    private val localDataSource: TranscriptLocalDataSource
) : TranscriptRepository {

    override fun getAllTranscripts(): Flow<List<Transcript>> {
        return localDataSource.getAllTranscripts()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getRecentTranscripts(limit: Int): Flow<List<Transcript>> {
        return localDataSource.getRecentTranscripts(limit)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getStats(): Flow<TranscriptStats> {
        return combine(
            localDataSource.getTranscriptsCount(),
            localDataSource.getTotalDuration(),
            localDataSource.getAllTranscriptTexts()
        ) { count, totalDuration, texts ->
            val totalWords = texts.sumOf { it.split("\\s+".toRegex()).size }
            TranscriptStats(
                totalCount = count,
                totalWords = totalWords,
                averageDurationMs = if (count > 0) totalDuration / count else 0L
            )
        }
    }

    override suspend fun saveTranscript(
        text: String,
        rawText: String,
        durationMs: Long,
        languageCode: String,
        preset: String
    ) {
        val entity = TranscriptEntity(
            text = text,
            rawText = rawText,
            timestampMs = System.currentTimeMillis(),
            durationMs = durationMs,
            languageCode = languageCode,
            preset = preset
        )
        localDataSource.insertTranscript(entity)
    }

    override suspend fun deleteTranscript(id: Long) =
        localDataSource.deleteTranscript(id)

    override suspend fun clearAll() =
        localDataSource.clearAll()

    override suspend fun deleteAll() =
        localDataSource.deleteAll()

    override suspend fun deleteTranscriptsOlderThan(thresholdMs: Long) =
        localDataSource.deleteOlderThan(thresholdMs)

    override suspend fun updatePinStatus(id: Long, isPinned: Boolean) =
        localDataSource.updatePinStatus(id, isPinned)

    override suspend fun updateTranscript(transcript: Transcript) {
        localDataSource.insertTranscript(transcript.toEntity())
    }

    override suspend fun getTranscriptById(id: Long): Transcript? =
        localDataSource.getTranscriptById(id)?.toDomain()
}