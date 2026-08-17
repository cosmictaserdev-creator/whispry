// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.updater

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadState {
    data class InProgress(val progressPct: Int) : DownloadState
    data class Done(val file: File) : DownloadState
    data class Failed(val reason: String) : DownloadState
}

/**
 * Downloads a release APK via the system [DownloadManager] rather than hand-rolled OkHttp
 * streaming — it's the native platform way to do this, handles progress and retries for us, and
 * the download survives the app being backgrounded mid-transfer.
 */
@Singleton
class UpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun download(url: String, fileName: String): Flow<DownloadState> = flow {
        val destination = File(context.getExternalFilesDir("updates"), fileName)
        destination.parentFile?.mkdirs()
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(url.toUri())
            .setTitle("Whispry update")
            .setDestinationUri(Uri.fromFile(destination))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)

        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)

        while (true) {
            val cursor = downloadManager.query(query)
            if (!cursor.moveToFirst()) {
                cursor.close()
                emit(DownloadState.Failed("Download was cancelled"))
                return@flow
            }
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            cursor.close()

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    emit(DownloadState.Done(destination))
                    return@flow
                }
                DownloadManager.STATUS_FAILED -> {
                    emit(DownloadState.Failed("Download failed"))
                    return@flow
                }
                else -> {
                    val pct = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    emit(DownloadState.InProgress(pct))
                }
            }
            delay(400)
        }
    }
}
