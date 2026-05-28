package com.example.whispry.data.local.datasource

import com.example.whispry.data.local.db.TranscriptDao
import com.example.whispry.data.local.db.TranscriptEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TranscriptLocalDataSource @Inject constructor(
    private val dao: TranscriptDao
) {

    fun getAllTranscripts(): Flow<List<TranscriptEntity>> =
        dao.getAllTranscripts()

    suspend fun insertTranscript(entity: TranscriptEntity) =
        dao.insertTranscript(entity)

    suspend fun deleteTranscript(id: Long) =
        dao.deleteTranscript(id)

    suspend fun clearAll() =
        dao.clearAll()

    suspend fun updatePinStatus(id: Long, isPinned: Boolean) =
        dao.updatePinStatus(id, isPinned)

    suspend fun getTranscriptById(id: Long): TranscriptEntity? =
        dao.getTranscriptById(id)
}