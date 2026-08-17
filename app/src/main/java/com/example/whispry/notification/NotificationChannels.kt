// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val FOREGROUND_SERVICE = "foreground_service"
    const val USAGE_ALERTS = "usage_alerts"
    const val PREMIUM_REMINDER = "premium_reminder"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                FOREGROUND_SERVICE,
                "Whispry Service",
                NotificationManager.IMPORTANCE_NONE
            ).apply {
                description = "Keeps the recording service running without showing in the shade"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                USAGE_ALERTS,
                "Usage Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications showing your daily transcription usage"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                PREMIUM_REMINDER,
                "Premium Features",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Discover Whispry's premium features"
            }
        )
    }
}
