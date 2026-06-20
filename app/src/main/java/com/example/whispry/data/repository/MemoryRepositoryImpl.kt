package com.example.whispry.data.repository

import com.example.whispry.data.local.db.MemoryDao
import com.example.whispry.data.local.db.MemoryEntity
import com.example.whispry.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val memoryDao: MemoryDao
) : MemoryRepository {
    override fun getAllMemoriesFlow(): Flow<List<MemoryEntity>> = memoryDao.getAllMemoriesFlow()
    override suspend fun getActiveMemories(): List<MemoryEntity> = memoryDao.getActiveMemories()
    override suspend fun saveMemory(memory: MemoryEntity) = memoryDao.insertMemory(memory)
    override suspend fun deleteMemory(memory: MemoryEntity) = memoryDao.deleteMemory(memory)
    override suspend fun deleteAllMemories() = memoryDao.deleteAllMemories()
}
