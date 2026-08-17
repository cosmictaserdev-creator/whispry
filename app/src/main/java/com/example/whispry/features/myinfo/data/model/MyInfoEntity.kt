// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.myinfo.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A saved personal value the user can paste by voice with "insert <key>".
 * e.g. key = "address", value = "123 Main St, Springfield".
 */
@Entity(
    tableName = "my_info",
    indices = [Index(value = ["key"], unique = true)]
)
data class MyInfoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val createdAt: Long = System.currentTimeMillis()
)
