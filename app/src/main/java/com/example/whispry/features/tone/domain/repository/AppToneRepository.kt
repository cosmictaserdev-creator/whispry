package com.example.whispry.features.tone.domain.repository

import com.example.whispry.features.tone.data.model.AppToneEntity
import kotlinx.coroutines.flow.Flow

interface AppToneRepository {
    fun getAllAppTones(): Flow<List<AppToneEntity>>
    suspend fun getAppToneByPackage(packageName: String): AppToneEntity?
    suspend fun saveAppTone(mapping: AppToneEntity)
    suspend fun deleteAppTone(packageName: String)
}
