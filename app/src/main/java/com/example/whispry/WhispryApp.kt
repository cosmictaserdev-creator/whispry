package com.example.whispry

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.work.*
import com.example.whispry.util.CleanupWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class WhispryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleCleanup()
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
}