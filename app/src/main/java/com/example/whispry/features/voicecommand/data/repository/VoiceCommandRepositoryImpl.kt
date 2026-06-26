package com.example.whispry.features.voicecommand.data.repository

import com.example.whispry.features.voicecommand.data.local.db.VoiceCommandDao
import com.example.whispry.features.voicecommand.data.model.VoiceCommandEntity
import com.example.whispry.features.voicecommand.domain.repository.VoiceCommandRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandRepositoryImpl @Inject constructor(
    private val dao: VoiceCommandDao
) : VoiceCommandRepository {
    override fun getAll(): Flow<List<VoiceCommandEntity>> = dao.getAllFlow()

    override suspend fun getByTrigger(word: String): VoiceCommandEntity? =
        dao.getByTrigger(word.trim().lowercase())

    override suspend fun save(triggerWord: String, action: String, targetPackage: String, targetAppLabel: String) {
        dao.insert(
            VoiceCommandEntity(
                triggerWord = triggerWord.trim().lowercase(),
                action = action,
                targetPackage = targetPackage,
                targetAppLabel = targetAppLabel
            )
        )
    }

    override suspend fun delete(entity: VoiceCommandEntity) = dao.delete(entity)
}
