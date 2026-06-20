package com.example.whispry.features.tone.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.whispry.features.tone.data.model.AppToneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppToneDao {

    @Query("SELECT * FROM app_tone_mappings ORDER BY appName ASC")
    fun getAllAppTones(): Flow<List<AppToneEntity>>

    @Query("SELECT * FROM app_tone_mappings WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppToneByPackage(packageName: String): AppToneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppTone(mapping: AppToneEntity)

    @Query("DELETE FROM app_tone_mappings WHERE packageName = :packageName")
    suspend fun deleteAppTone(packageName: String)
}
