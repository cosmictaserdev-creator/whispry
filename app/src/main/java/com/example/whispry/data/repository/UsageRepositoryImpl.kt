// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.data.repository

import com.example.whispry.data.local.datasource.UsageDataStore
import com.example.whispry.domain.model.UsageInfo
import com.example.whispry.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val usageDataStore: UsageDataStore
) : UsageRepository {

    override suspend fun incrementRequests(count: Int) {
        usageDataStore.ensureNewDay()
        usageDataStore.incrementRequests(count)
    }

    override suspend fun incrementWords(count: Int) {
        usageDataStore.ensureNewDay()
        usageDataStore.incrementWords(count)
    }

    override suspend fun getTodayUsage(): UsageInfo {
        usageDataStore.ensureNewDay()
        return usageDataStore.getUsage()
    }

    override fun observeTodayUsage(): Flow<UsageInfo> {
        return usageDataStore.observeUsage()
    }

    override suspend fun resetIfNewDay() {
        usageDataStore.ensureNewDay()
    }
}
