package com.example.whispry

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.work.*
import com.example.whispry.service.ServiceWatchdogWorker
import com.example.whispry.service.TranscriptCleanupWorker
import com.example.whispry.ui.util.liquid.GlassBackdropCache
import com.example.whispry.util.CleanupWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class WhispryApp : Application() {
    @Inject lateinit var glassBackdropCache: GlassBackdropCache

    override fun onCreate() {
        super.onCreate()
        glassBackdropCache.init()
        scheduleCleanup()
        scheduleWatchdog()
        scheduleTranscriptCleanup()
    }

    private fun scheduleTranscriptCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<TranscriptCleanupWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "transcript_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

    private fun scheduleCleanup() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AudioCleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

    private fun scheduleWatchdog() {
        val watchdogRequest = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
            15, TimeUnit.MINUTES  // minimum interval WorkManager allows
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false) // check even on low battery
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "service_watchdog",
            ExistingPeriodicWorkPolicy.KEEP,
            watchdogRequest
        )
    }
}