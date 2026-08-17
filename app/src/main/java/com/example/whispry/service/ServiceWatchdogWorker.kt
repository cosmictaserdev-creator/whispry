// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.whispry.MainActivity
import com.example.whispry.R

class ServiceWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WATCHDOG_CHANNEL_ID = "service_watchdog_channel"
        private const val ACCESSIBILITY_NOTIFICATION_ID = 101
        private const val SERVICE_NOTIFICATION_ID = 102
    }

    override suspend fun doWork(): Result {
        val isServiceRunning = isServiceRunning(BubbleService::class.java)
        val isAccessibilityEnabled = isAccessibilityEnabled()
        
        when {
            !isAccessibilityEnabled -> {
                showAccessibilityRevokedNotification()
            }
            !isServiceRunning -> {
                try {
                    val intent = Intent(applicationContext, BubbleService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        applicationContext.startForegroundService(intent)
                    } else {
                        applicationContext.startService(intent)
                    }
                } catch (e: Exception) {
                    showServiceStoppedNotification()
                }
            }
        }
        return Result.success()
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val am = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return am.getRunningServices(Int.MAX_VALUE)
            ?.any { it.service.className == serviceClass.name } == true
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(applicationContext.packageName)
    }

    private fun showAccessibilityRevokedNotification() {
        createNotificationChannel()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("SHOW_ACCESSIBILITY_PROMPT", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, WATCHDOG_CHANNEL_ID)
            .setContentTitle("Whispry needs accessibility access")
            .setContentText("The voice trigger has stopped working. Tap to re-enable.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ACCESSIBILITY_NOTIFICATION_ID, notification)
    }

    private fun showServiceStoppedNotification() {
        createNotificationChannel()
        val intent = Intent(applicationContext, BubbleService::class.java)
        val pendingIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(applicationContext, WATCHDOG_CHANNEL_ID)
            .setContentTitle("Whispry is paused")
            .setContentText("Tap to restart the voice trigger.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(SERVICE_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WATCHDOG_CHANNEL_ID,
                "Service Watchdog",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
