package com.example.whispry.data.local.db


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {

    @Query("SELECT * FROM transcripts ORDER BY timestampMs DESC")
    fun getAllTranscripts(): Flow<List<TranscriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(entity: TranscriptEntity)

    @Query("DELETE FROM transcripts WHERE id = :id")
    suspend fun deleteTranscript(id: Long)

    @Query("DELETE FROM transcripts WHERE isPinned = 0")
    suspend fun clearAll()

    @Query("UPDATE transcripts SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: Long, isPinned: Boolean)

    @Query("SELECT * FROM transcripts WHERE id = :id LIMIT 1")
    suspend fun getTranscriptById(id: Long): TranscriptEntity?
}