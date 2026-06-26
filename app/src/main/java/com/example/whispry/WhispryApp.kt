package com.example.whispry

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.work.*
import com.example.whispry.notification.NotificationChannels
import com.example.whispry.notification.PremiumReminderWorker
import com.example.whispry.service.ServiceWatchdogWorker
import com.example.whispry.service.TranscriptCleanupWorker
import com.example.whispry.ui.util.liquid.GlassBackdropCache
import com.example.whispry.util.CleanupWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltAndroidApp
class WhispryApp : Application() {
    @Inject lateinit var glassBackdropCache: GlassBackdropCache
    @Inject lateinit var defaultsSeeder: com.example.whispry.data.local.DefaultsSeeder

    override fun onCreate() {
        super.onCreate()
        glassBackdropCache.init()
        NotificationChannels.createAll(this)
        scheduleCleanup()
        scheduleWatchdog()
        scheduleTranscriptCleanup()
        schedulePremiumReminder()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try { defaultsSeeder.seedIfNeeded() } catch (_: Exception) { }
        }
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

    private fun schedulePremiumReminder() {
        val reminderRequest = PeriodicWorkRequestBuilder<PremiumReminderWorker>(
            2, TimeUnit.DAYS
        )
            .setInitialDelay(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "premium_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}