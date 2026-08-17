// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class CleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("CleanupWorker", "Starting audio cache cleanup")
        return try {
            val recordingsDir = File(applicationContext.cacheDir, "recordings")
            if (recordingsDir.exists() && recordingsDir.isDirectory) {
                val files = recordingsDir.listFiles()
                val now = System.currentTimeMillis()
                files?.forEach { file ->
                    // Delete files older than 24 hours
                    if (now - file.lastModified() > 24 * 60 * 60 * 1000) {
                        Log.d("CleanupWorker", "Deleting old recording: ${file.name}")
                        file.delete()
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("CleanupWorker", "Error during cleanup", e)
            Result.failure()
        }
    }
}
