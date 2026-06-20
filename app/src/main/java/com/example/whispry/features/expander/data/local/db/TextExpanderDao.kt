package com.example.whispry.features.expander.data.local.db

import androidx.room.*
import com.example.whispry.features.expander.data.model.TextExpanderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TextExpanderDao {
    @Query("SELECT * FROM text_expanders ORDER BY createdAt DESC")
    fun getAllExpandersFlow(): Flow<List<TextExpanderEntity>>

    @Query("SELECT * FROM text_expanders WHERE shortcut = :shortcut LIMIT 1")
    suspend fun getExpanderByShortcut(shortcut: String): TextExpanderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpander(expander: TextExpanderEntity)

    @Delete
    suspend fun deleteExpander(expander: TextExpanderEntity)
}
