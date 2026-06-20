package com.example.whispry.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_bank ORDER BY createdAt DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory_bank WHERE isActive = 1")
    suspend fun getActiveMemories(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memory_bank")
    suspend fun deleteAllMemories()
}
