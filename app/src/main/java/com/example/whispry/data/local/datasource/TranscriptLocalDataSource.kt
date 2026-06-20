package com.example.whispry.data.local.datasource

import com.example.whispry.data.local.db.TranscriptDao
import com.example.whispry.data.local.db.TranscriptEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TranscriptLocalDataSource @Inject constructor(
    private val dao: TranscriptDao
) {

    fun getAllTranscripts(): Flow<List<TranscriptEntity>> =
        dao.getAllTranscripts()

    fun getRecentTranscripts(limit: Int): Flow<List<TranscriptEntity>> =
        dao.getRecentTranscripts(limit)

    fun getTranscriptsCount(): Flow<Int> =
        dao.getTranscriptsCount()

    fun getTotalDuration(): Flow<Long> =
        dao.getTotalDuration().map { it ?: 0L }

    fun getAllTranscriptTexts(): Flow<List<String>> =
        dao.getAllTranscriptTexts()

    suspend fun insertTranscript(entity: TranscriptEntity) =
        dao.insertTranscript(entity)

    suspend fun deleteTranscript(id: Long) =
        dao.deleteTranscript(id)

    suspend fun clearAll() =
        dao.clearAll()

    suspend fun deleteAll() =
        dao.deleteAll()

    suspend fun deleteOlderThan(threshold: Long) =
        dao.deleteOlderThan(threshold)

    suspend fun updatePinStatus(id: Long, isPinned: Boolean) =
        dao.updatePinStatus(id, isPinned)

    suspend fun getTranscriptById(id: Long): TranscriptEntity? =
        dao.getTranscriptById(id)
}