package com.example.whispry.features.voicecommand.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A first-word voice command. Saying "<triggerWord> <query>" runs [action] with the query.
 * [targetPackage]/[targetAppLabel] are only used for the OPEN_APP action.
 */
@Entity(
    tableName = "voice_commands",
    indices = [Index(value = ["triggerWord"], unique = true)]
)
data class VoiceCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerWord: String,
    val action: String,           // VoiceCommandAction.name
    val targetPackage: String = "",
    val targetAppLabel: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
