package com.example.whispry.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.domain.model.RetentionPolicy
import com.example.whispry.domain.repository.TranscriptRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class TranscriptCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TranscriptCleanupEntryPoint {
        fun transcriptRepository(): TranscriptRepository
        fun settingsProvider(): com.example.whispry.data.local.datasource.SettingsProvider
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                TranscriptCleanupEntryPoint::class.java
            )
            val transcriptRepository = entryPoint.transcriptRepository()
            val settingsProvider = entryPoint.settingsProvider()

            val prefs = settingsProvider.dataStore.data.first()
            val policyName = prefs[DataStoreKeys.RETENTION_POLICY] ?: RetentionPolicy.FOREVER.name
            val policy = try {
                RetentionPolicy.valueOf(policyName)
            } catch (e: Exception) {
                RetentionPolicy.FOREVER
            }

            if (policy.days == null) return Result.success() // FOREVER — skip

            val thresholdMs = System.currentTimeMillis() -
                (policy.days.toLong() * 24 * 60 * 60 * 1000L)

            transcriptRepository.deleteTranscriptsOlderThan(thresholdMs)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
