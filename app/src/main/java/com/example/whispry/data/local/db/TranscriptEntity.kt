// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val timestampMs: Long,
    val durationMs: Long,
    val languageCode: String,
    val isPinned: Boolean = false,
    val rawText: String = "",
    val preset: String = "NONE"
)