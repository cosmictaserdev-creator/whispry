// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.myinfo.domain.repository

import com.example.whispry.features.myinfo.data.model.MyInfoEntity
import kotlinx.coroutines.flow.Flow

interface MyInfoRepository {
    fun getAll(): Flow<List<MyInfoEntity>>
    suspend fun getValueForKey(key: String): String?
    suspend fun save(key: String, value: String)
    suspend fun delete(entity: MyInfoEntity)
}
