package com.example.whispry

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.work.*
import com.example.whispry.service.ServiceWatchdogWorker
import com.example.whispry.util.CleanupWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class WhispryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleCleanup()
        scheduleWatchdog()
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