// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.util

import android.content.Context
import android.os.Build
import com.example.whispry.BuildConfig
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * On-device-only crash capture: no crash-reporting SDK or account, nothing leaves the device.
 * Writes the stack trace + basic device/app info to a local file the user can attach when
 * reporting a bug (see AboutScreen's "Share Crash Log" row), then hands off to whatever the
 * previous uncaught-exception handler was — this only records, it never swallows the crash.
 */
object CrashLogger {

    private const val DIR_NAME = "crash_logs"
    private const val MAX_LOGS = 5

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashLog(appContext, thread, throwable)
            } catch (e: Exception) {
                Timber.e(e, "Failed to write crash log")
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val file = File(dir, "crash_$timestamp.txt")

        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        file.writeText(
            buildString {
                appendLine("Whispry ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Thread: ${thread.name}")
                appendLine("Time: $timestamp")
                appendLine()
                append(stackTrace)
            }
        )

        // Keep only the most recent MAX_LOGS files so this can't grow unbounded.
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(MAX_LOGS)?.forEach { it.delete() }
    }

    /** Most recent crash log, if any. */
    fun latestCrashLog(context: Context): File? =
        File(context.filesDir, DIR_NAME).listFiles()?.maxByOrNull { it.lastModified() }
}
