// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.features.myinfo.data.local.db

import androidx.room.*
import com.example.whispry.features.myinfo.data.model.MyInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MyInfoDao {
    @Query("SELECT * FROM my_info ORDER BY createdAt ASC")
    fun getAllFlow(): Flow<List<MyInfoEntity>>

    @Query("SELECT * FROM my_info WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): MyInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MyInfoEntity)

    @Delete
    suspend fun delete(entity: MyInfoEntity)
}
