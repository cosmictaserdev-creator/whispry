package com.example.whispry.features.tone.data.repository

import com.example.whispry.features.tone.data.local.db.AppToneDao
import com.example.whispry.features.tone.data.model.AppToneEntity
import com.example.whispry.features.tone.domain.repository.AppToneRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppToneRepositoryImpl @Inject constructor(
    private val appToneDao: AppToneDao
) : AppToneRepository {

    override fun getAllAppTones(): Flow<List<AppToneEntity>> {
        return appToneDao.getAllAppTones()
    }

    override suspend fun getAppToneByPackage(packageName: String): AppToneEntity? {
        return appToneDao.getAppToneByPackage(packageName)
    }

    override suspend fun saveAppTone(mapping: AppToneEntity) {
        appToneDao.insertAppTone(mapping)
    }

    override suspend fun deleteAppTone(packageName: String) {
        appToneDao.deleteAppTone(packageName)
    }
}
