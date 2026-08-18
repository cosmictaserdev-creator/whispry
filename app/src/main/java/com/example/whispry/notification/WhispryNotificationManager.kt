// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.whispry.R
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.UsageInfo
import com.example.whispry.ui.theme.AccentPreset
import com.example.whispry.ui.theme.resolveAccentColors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhispryNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider
) {

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1
        const val PREMIUM_REMINDER_NOTIFICATION_ID = 201
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cachedAccentColor: Int = AccentPreset.Purple.mainColor.hashCode()

    init {
        scope.launch {
            try {
                val accentName = settingsProvider.accentColor.first()
                cachedAccentColor = resolveAccentColors(accentName).mainColor.hashCode()
            } catch (_: Exception) { }
        }
    }

    fun createChannels() {
        NotificationChannels.createAll(context)
    }

    fun buildFallbackNotification(): Notification {
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse("whispry://home")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NotificationChannels.FOREGROUND_SERVICE)
            .setContentTitle("Whispry is active")
            .setContentText("Ready to capture")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setColor(cachedAccentColor)
            .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setColorized(true) }
            .build()
    }

    fun buildForegroundNotification(usageInfo: UsageInfo): Notification {
        val percent = (usageInfo.requestsPercent * 100).toInt()
        val title = "Whispry \u00B7 $percent% used today"
        val text = buildString {
            append("${usageInfo.requestsUsed} / ${usageInfo.dailyLimit} requests")
            if (usageInfo.wordsUsed > 0) {
                append(" \u00B7 ${usageInfo.wordsUsed} words")
            }
        }

        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse("whispry://home")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NotificationChannels.FOREGROUND_SERVICE)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setColor(cachedAccentColor)
            .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setColorized(true) }
            .build()
    }

    fun updateForegroundNotification(usageInfo: UsageInfo) {
        try {
            val notification = buildForegroundNotification(usageInfo)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(FOREGROUND_NOTIFICATION_ID, notification)
        } catch (_: Exception) { }
    }

    fun buildPremiumReminderNotification(
        title: String,
        body: String,
        deepLinkHost: String
    ): Notification {
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse("whispry://$deepLinkHost")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, deepLinkHost.hashCode(), deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NotificationChannels.PREMIUM_REMINDER)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(cachedAccentColor)
            .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setColorized(true) }
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    fun buildUploadResultNotification(success: Boolean, message: String): Notification {
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse("whispry://history")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, message.hashCode(), deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NotificationChannels.FILE_TRANSCRIPTION)
            .setContentTitle(if (success) "Transcription ready" else "Upload failed")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(cachedAccentColor)
            .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setColorized(true) }
            .build()
    }

    fun showUploadResult(success: Boolean, message: String, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).notify(
                notificationId,
                buildUploadResultNotification(success, message)
            )
        } catch (_: Exception) { }
    }

    fun showPremiumReminder(title: String, body: String, deepLinkHost: String) {
        try {
            val notification = buildPremiumReminderNotification(title, body, deepLinkHost)
            NotificationManagerCompat.from(context).notify(
                PREMIUM_REMINDER_NOTIFICATION_ID,
                notification
            )
        } catch (_: Exception) { }
    }
}
