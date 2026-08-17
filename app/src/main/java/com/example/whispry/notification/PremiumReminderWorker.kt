// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class PremiumReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PremiumReminderEntryPoint {
        fun notificationManager(): WhispryNotificationManager
        fun settingsProvider(): SettingsProvider
    }

    private data class ReminderMessage(
        val title: String,
        val body: String,
        val deepLinkHost: String
    )

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PremiumReminderEntryPoint::class.java
        )
        val settingsProvider = entryPoint.settingsProvider()
        val enabled = settingsProvider.dataStore.data.first()[DataStoreKeys.PREMIUM_REMINDERS_ENABLED]
            ?: DataStoreKeys.DEFAULT_PREMIUM_REMINDERS_ENABLED
        if (!enabled) return Result.success()

        val notificationManager = entryPoint.notificationManager()

        val index = (runAttemptCount + System.currentTimeMillis() / 86400000L).toInt() % messages.size
        val msg = messages[index]
        notificationManager.showPremiumReminder(msg.title, msg.body, msg.deepLinkHost)
        return Result.success()
    }

    companion object {
        private val messages = listOf(
            ReminderMessage(
                "Unlock Smart Formatting",
                "Transform your transcripts with AI-powered Output Presets. Choose from Professional, Friendly, Concise, and more.",
                "presets"
            ),
            ReminderMessage(
                "Text Expander - Type Less, Say More",
                "Create custom shortcuts that expand into full sentences. Perfect for common responses and templates.",
                "text-expander"
            ),
            ReminderMessage(
                "Never Forget with Memory",
                "Whispry can remember key information about you for more contextual transcription results.",
                "memory"
            ),
            ReminderMessage(
                "Custom App Tones",
                "Set unique transcription tones for different apps. Know which app gets your text without looking.",
                "app-tones"
            ),
            ReminderMessage(
                "Tasker Integration",
                "Format your transcripts for Tasker automation. Trigger tasks with your voice like never before.",
                "settings"
            ),
            ReminderMessage(
                "Advanced Voice Training",
                "Improve wake word accuracy with voice training. Your Whispry, your voice, perfect recognition.",
                "settings"
            ),
            ReminderMessage(
                "Audio Ducking",
                "Whispry automatically lowers media volume during recording so nothing gets missed.",
                "settings"
            ),
            ReminderMessage(
                "Export & Backup",
                "Export your transcripts to share or backup. Never lose an important transcription again.",
                "history"
            ),
            ReminderMessage(
                "Unlimited Possibilities",
                "Whispry's full feature set works together seamlessly. Voice trigger + presets + expander = ultimate productivity.",
                "home"
            )
        )
    }
}
