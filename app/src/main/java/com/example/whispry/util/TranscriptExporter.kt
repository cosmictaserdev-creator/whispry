package com.example.whispry.util

import com.example.whispry.domain.model.Transcript
import java.util.Locale

enum class ExportFormat(
    val extension: String,
    val displayName: String,
    val description: String,
    val mimeType: String
) {
    TXT("txt", "Plain Text (.txt)", "Standard plain text transcription", "text/plain"),
    SRT("srt", "SubRip Subtitles (.srt)", "Subtitle format with time markers", "text/plain"),
    VTT("vtt", "WebVTT Subtitles (.vtt)", "Web-compatible subtitle format", "text/vtt"),
    JSON("json", "JSON Data (.json)", "Structured transcription metadata", "application/json"),
    CSV("csv", "CSV Spreadsheet (.csv)", "Row format for spreadsheets/archiving", "text/csv")
}

object TranscriptExporter {

    fun toTxt(transcript: Transcript): String {
        return transcript.text
    }

    fun toSrt(transcript: Transcript): String {
        val text = transcript.text
        val durationMs = transcript.durationMs
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return ""

        val wordsPerSegment = 8
        val segments = words.chunked(wordsPerSegment)
        val segmentDurationMs = if (segments.isNotEmpty()) durationMs / segments.size else durationMs

        val sb = StringBuilder()
        for (i in segments.indices) {
            val segmentText = segments[i].joinToString(" ")
            val startMs = i * segmentDurationMs
            val endMs = ((i + 1) * segmentDurationMs).coerceAtMost(durationMs)

            sb.append(i + 1).append("\n")
            sb.append(formatSrtTime(startMs)).append(" --> ").append(formatSrtTime(endMs)).append("\n")
            sb.append(segmentText).append("\n\n")
        }
        return sb.toString().trim()
    }

    fun toVtt(transcript: Transcript): String {
        val text = transcript.text
        val durationMs = transcript.durationMs
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return "WEBVTT\n\n"

        val wordsPerSegment = 8
        val segments = words.chunked(wordsPerSegment)
        val segmentDurationMs = if (segments.isNotEmpty()) durationMs / segments.size else durationMs

        val sb = StringBuilder()
        sb.append("WEBVTT\n\n")
        for (i in segments.indices) {
            val segmentText = segments[i].joinToString(" ")
            val startMs = i * segmentDurationMs
            val endMs = ((i + 1) * segmentDurationMs).coerceAtMost(durationMs)

            sb.append(formatVttTime(startMs)).append(" --> ").append(formatVttTime(endMs)).append("\n")
            sb.append(segmentText).append("\n\n")
        }
        return sb.toString().trim()
    }

    fun toJson(transcript: Transcript): String {
        return """{
  "id": ${transcript.id},
  "timestampMs": ${transcript.timestampMs},
  "date": "${transcript.createdAtFormatted}",
  "durationMs": ${transcript.durationMs},
  "languageCode": "${transcript.languageCode}",
  "preset": "${transcript.preset}",
  "text": "${escapeJson(transcript.text)}",
  "rawText": "${escapeJson(transcript.rawText)}"
}"""
    }

    fun toCsv(transcript: Transcript): String {
        val sb = StringBuilder()
        sb.append("id,timestampMs,date,durationMs,languageCode,preset,text,rawText\n")
        sb.append(transcript.id).append(",")
        sb.append(transcript.timestampMs).append(",")
        sb.append("\"").append(transcript.createdAtFormatted).append("\",")
        sb.append(transcript.durationMs).append(",")
        sb.append("\"").append(transcript.languageCode).append("\",")
        sb.append("\"").append(transcript.preset).append("\",")
        sb.append("\"").append(escapeCsv(transcript.text)).append("\",")
        sb.append("\"").append(escapeCsv(transcript.rawText)).append("\"")
        return sb.toString()
    }

    private fun formatSrtTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    private fun formatVttTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }
}
