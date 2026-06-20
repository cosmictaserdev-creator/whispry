package com.example.whispry.domain.repository

import com.example.whispry.data.local.db.MemoryEntity
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>
    suspend fun getActiveMemories(): List<MemoryEntity>
    suspend fun saveMemory(memory: MemoryEntity)
    suspend fun deleteMemory(memory: MemoryEntity)
    suspend fun deleteAllMemories()
}
