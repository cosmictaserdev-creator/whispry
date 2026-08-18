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
            deleteOlderThan(File(applicationContext.cacheDir, "recordings"), "recording")
            // Upload temp files normally self-delete when UploadTranscribeWorker finishes, but a
            // process death mid-doWork() (before its finally runs) can orphan one — sweep those too.
            deleteOlderThan(File(applicationContext.cacheDir, "uploads"), "upload")
            Result.success()
        } catch (e: Exception) {
            Log.e("CleanupWorker", "Error during cleanup", e)
            Result.failure()
        }
    }

    private fun deleteOlderThan(dir: File, label: String) {
        if (!dir.exists() || !dir.isDirectory) return
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            // Delete files older than 24 hours
            if (now - file.lastModified() > 24 * 60 * 60 * 1000) {
                Log.d("CleanupWorker", "Deleting old $label: ${file.name}")
                file.delete()
            }
        }
    }
}
