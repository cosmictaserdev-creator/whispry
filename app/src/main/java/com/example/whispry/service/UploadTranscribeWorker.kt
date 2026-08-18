// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.repository.AudioRepository
import com.example.whispry.domain.repository.TranscriptRepository
import com.example.whispry.domain.repository.UsageRepository
import com.example.whispry.notification.WhispryNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.io.File
import com.example.whispry.domain.util.Result as DomainResult

/** Transcribes a user-picked audio file in the background (survives app kill/backgrounding),
 *  reusing the same Groq pipeline and history/usage bookkeeping as a live recording. */
class UploadTranscribeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UploadTranscribeEntryPoint {
        fun audioRepository(): AudioRepository
        fun transcriptRepository(): TranscriptRepository
        fun usageRepository(): UsageRepository
        fun settingsProvider(): SettingsProvider
        fun notificationManager(): WhispryNotificationManager
    }

    companion object {
        const val KEY_FILE_PATH = "file_path"
        const val KEY_DISPLAY_NAME = "display_name"

        // Groq's free-tier direct-upload cap (Dev tier allows 100MB via the same endpoint).
        // ponytail: hard reject over this rather than chunk/downsample; wire chunking (see Groq's
        // cookbook) or the `url` param if longer files become a real ask.
        private const val MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024
        private val SUPPORTED_EXTENSIONS =
            setOf("flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "wav", "webm")

        // Error messages GroqRemoteDataSource returns for a dropped connection / timeout rather
        // than an actual Groq API rejection (bad key, unsupported params, rate limit) — those are
        // permanent and shouldn't burn retries. See GroqRemoteDataSource.transcribeAudio.
        private val TRANSIENT_ERROR_PREFIXES = listOf("no_internet", "No internet connection", "Error:")
        private const val MAX_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val displayName = inputData.getString(KEY_DISPLAY_NAME) ?: "Uploaded audio"
        val file = File(filePath)
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            UploadTranscribeEntryPoint::class.java
        )
        val notificationManager = entryPoint.notificationManager()
        val notificationId = filePath.hashCode()

        if (!file.exists()) return Result.failure()

        if (file.extension.lowercase() !in SUPPORTED_EXTENSIONS) {
            notificationManager.showUploadResult(
                false, "$displayName: unsupported audio format", notificationId
            )
            file.delete()
            return Result.failure()
        }
        if (file.length() > MAX_FILE_SIZE_BYTES) {
            notificationManager.showUploadResult(
                false, "$displayName: file exceeds the 25MB upload limit", notificationId
            )
            file.delete()
            return Result.failure()
        }

        val settingsProvider = entryPoint.settingsProvider()
        val language = settingsProvider.language.first()
        val languageCode = if (language == "Auto") "en" else language

        var isRetry = false
        val result = when (val transcribed = entryPoint.audioRepository().transcribeAudio(file.absolutePath, languageCode)) {
            is DomainResult.Success -> {
                val text = transcribed.data
                val durationMs = readDurationMs(file.absolutePath)

                entryPoint.transcriptRepository().saveTranscript(
                    text = text,
                    rawText = text,
                    durationMs = durationMs,
                    languageCode = language,
                    preset = OutputPreset.NONE.name
                )

                val usageRepository = entryPoint.usageRepository()
                usageRepository.incrementRequests(1)
                val wordCount = text.split("\\s+".toRegex()).count { it.isNotBlank() }
                if (wordCount > 0) usageRepository.incrementWords(wordCount)

                notificationManager.showUploadResult(true, "$displayName transcribed", notificationId)
                Result.success()
            }
            is DomainResult.Error -> {
                val isTransient = TRANSIENT_ERROR_PREFIXES.any { transcribed.message.startsWith(it) }
                if (isTransient && runAttemptCount < MAX_ATTEMPTS) {
                    // Quiet retry — the file stays put; no point alarming the user over one dropped
                    // connection when WorkManager is about to try again on its own.
                    isRetry = true
                    Result.retry()
                } else {
                    notificationManager.showUploadResult(
                        false, "$displayName: ${transcribed.message}", notificationId
                    )
                    Result.failure()
                }
            }
            else -> Result.failure()
        }

        // Retry needs the file to still be there for the next attempt — only clean up once this
        // upload has truly finished, one way or the other. (Can't pattern-match on Result.Retry
        // directly: it's @RestrictTo(LIBRARY_GROUP) in WorkManager, app code isn't allowed to
        // reference the concrete subtype, only the isRetry flag set when we chose Result.retry().)
        if (!isRetry) file.delete()
        return result
    }

    private fun readDurationMs(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }
}
