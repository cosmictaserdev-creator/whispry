// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.expander.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "text_expanders",
    indices = [Index(value = ["shortcut"], unique = true)]
)
data class TextExpanderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shortcut: String,
    val expansion: String,
    val createdAt: Long = System.currentTimeMillis()
)
