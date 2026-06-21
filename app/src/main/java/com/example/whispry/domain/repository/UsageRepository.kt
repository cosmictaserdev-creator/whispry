package com.example.whispry.domain.repository

import com.example.whispry.domain.model.UsageInfo
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    suspend fun incrementRequests(count: Int = 1)
    suspend fun incrementWords(count: Int)
    suspend fun getTodayUsage(): UsageInfo
    fun observeTodayUsage(): Flow<UsageInfo>
    suspend fun resetIfNewDay()
}
