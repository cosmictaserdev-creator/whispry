package com.example.whispry.data.local.db

import com.example.whispry.data.local.db.TranscriptEntity
import com.example.whispry.domain.model.Transcript

// Extension functions — clean Kotlin way to add conversion to a class
// without modifying it

fun TranscriptEntity.toDomain(): Transcript {
    return Transcript(
        id = id,
        text = text,
        timestampMs = timestampMs,
        durationMs = durationMs,
        languageCode = languageCode,
        isPinned = isPinned
    )
}

fun Transcript.toEntity(): TranscriptEntity {
    return TranscriptEntity(
        id = id,
        text = text,
        timestampMs = timestampMs,
        durationMs = durationMs,
        languageCode = languageCode
    )
}