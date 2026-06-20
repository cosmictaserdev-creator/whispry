package com.example.whispry.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_bank")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "Personal",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
