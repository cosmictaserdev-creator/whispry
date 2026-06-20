package com.example.whispry.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Transcript(
    val id: Long = 0,
    val text: String,
    val timestampMs: Long,
    val durationMs: Long,
    val languageCode: String = "en",
    val isPinned: Boolean = false,
    val rawText: String = "",
    val preset: String = "NONE"
) {
    val createdAtFormatted: String
        get() {
            val date = Date(timestampMs)
            val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return format.format(date)
        }

    val relativeTime: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - timestampMs
            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                diff < 172800000 -> "Yesterday"
                else -> {
                    val format = SimpleDateFormat("MMM dd", Locale.getDefault())
                    format.format(Date(timestampMs))
                }
            }
            }
            }

            data class TranscriptStats(
            val totalCount: Int,
            val totalWords: Int,
            val averageDurationMs: Long
            )