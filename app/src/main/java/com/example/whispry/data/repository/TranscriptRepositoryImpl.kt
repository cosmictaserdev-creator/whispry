package com.example.whispry.data.repository

import com.example.whispry.data.local.datasource.TranscriptLocalDataSource
import com.example.whispry.data.local.db.TranscriptEntity
import com.example.whispry.data.local.db.toDomain
import com.example.whispry.domain.model.Transcript
import com.example.whispry.domain.repository.TranscriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TranscriptRepositoryImpl @Inject constructor(
    private val localDataSource: TranscriptLocalDataSource
) : TranscriptRepository {

    override fun getAllTranscripts(): Flow<List<Transcript>> {
        return localDataSource.getAllTranscripts()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun saveTranscript(
        text: String,
        durationMs: Long,
        languageCode: String
    ) {
        val entity = TranscriptEntity(
            text = text,
            timestampMs = System.currentTimeMillis(),
            durationMs = durationMs,
            languageCode = languageCode
        )
        localDataSource.insertTranscript(entity)
    }

    override suspend fun deleteTranscript(id: Long) =
        localDataSource.deleteTranscript(id)

    override suspend fun clearAll() =
        localDataSource.clearAll()

    override suspend fun updatePinStatus(id: Long, isPinned: Boolean) =
        localDataSource.updatePinStatus(id, isPinned)

    override suspend fun getTranscriptById(id: Long): Transcript? =
        localDataSource.getTranscriptById(id)?.toDomain()
}