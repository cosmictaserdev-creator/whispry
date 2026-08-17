// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.voicecommand.data.local.db

import androidx.room.*
import com.example.whispry.features.voicecommand.data.model.VoiceCommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceCommandDao {
    @Query("SELECT * FROM voice_commands ORDER BY createdAt ASC")
    fun getAllFlow(): Flow<List<VoiceCommandEntity>>

    @Query("SELECT * FROM voice_commands")
    suspend fun getAll(): List<VoiceCommandEntity>

    @Query("SELECT * FROM voice_commands WHERE triggerWord = :word LIMIT 1")
    suspend fun getByTrigger(word: String): VoiceCommandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VoiceCommandEntity)

    @Delete
    suspend fun delete(entity: VoiceCommandEntity)
}
