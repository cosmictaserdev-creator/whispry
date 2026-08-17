// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.voicecommand.domain.repository

import com.example.whispry.features.voicecommand.data.model.VoiceCommandEntity
import kotlinx.coroutines.flow.Flow

interface VoiceCommandRepository {
    fun getAll(): Flow<List<VoiceCommandEntity>>
    suspend fun getByTrigger(word: String): VoiceCommandEntity?
    suspend fun save(triggerWord: String, action: String, targetPackage: String, targetAppLabel: String)
    suspend fun delete(entity: VoiceCommandEntity)
}
