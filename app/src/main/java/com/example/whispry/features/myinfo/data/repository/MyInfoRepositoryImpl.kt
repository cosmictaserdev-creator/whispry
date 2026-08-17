// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.myinfo.data.repository

import com.example.whispry.features.myinfo.data.local.db.MyInfoDao
import com.example.whispry.features.myinfo.data.model.MyInfoEntity
import com.example.whispry.features.myinfo.domain.repository.MyInfoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyInfoRepositoryImpl @Inject constructor(
    private val dao: MyInfoDao
) : MyInfoRepository {
    override fun getAll(): Flow<List<MyInfoEntity>> = dao.getAllFlow()

    override suspend fun getValueForKey(key: String): String? =
        dao.getByKey(key.trim().lowercase())?.value?.takeIf { it.isNotBlank() }

    override suspend fun save(key: String, value: String) {
        dao.insert(MyInfoEntity(key = key.trim().lowercase(), value = value.trim()))
    }

    override suspend fun delete(entity: MyInfoEntity) = dao.delete(entity)
}
