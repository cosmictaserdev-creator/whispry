// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.data.local.db


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.whispry.features.expander.data.local.db.TextExpanderDao
import com.example.whispry.features.expander.data.model.TextExpanderEntity
import com.example.whispry.features.myinfo.data.local.db.MyInfoDao
import com.example.whispry.features.myinfo.data.model.MyInfoEntity
import com.example.whispry.features.tone.data.local.db.AppToneDao
import com.example.whispry.features.tone.data.model.AppToneEntity
import com.example.whispry.features.voicecommand.data.local.db.VoiceCommandDao
import com.example.whispry.features.voicecommand.data.model.VoiceCommandEntity

@Database(
    entities = [
        TranscriptEntity::class, TextExpanderEntity::class, AppToneEntity::class, MemoryEntity::class,
        MyInfoEntity::class, VoiceCommandEntity::class
    ],
    version = 8,
    exportSchema = true          // exports schema to a JSON file for migration tracking
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transcriptDao(): TranscriptDao
    abstract fun textExpanderDao(): TextExpanderDao
    abstract fun appToneDao(): AppToneDao
    abstract fun memoryDao(): MemoryDao
    abstract fun myInfoDao(): MyInfoDao
    abstract fun voiceCommandDao(): VoiceCommandDao

    companion object {
        const val DATABASE_NAME = "whispry_database"

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transcripts ADD COLUMN rawText TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE transcripts ADD COLUMN preset TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `text_expanders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `shortcut` TEXT NOT NULL, 
                        `expansion` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_text_expanders_shortcut` ON `text_expanders` (`shortcut`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_tone_mappings` (
                        `packageName` TEXT NOT NULL, 
                        `appName` TEXT NOT NULL, 
                        `presetName` TEXT NOT NULL, 
                        `customPromptOverride` TEXT NOT NULL DEFAULT '', 
                        PRIMARY KEY(`packageName`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `memory_bank` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `key` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `category` TEXT NOT NULL DEFAULT 'Personal',
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `my_info` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `key` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_my_info_key` ON `my_info` (`key`)")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `voice_commands` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `triggerWord` TEXT NOT NULL,
                        `action` TEXT NOT NULL,
                        `targetPackage` TEXT NOT NULL DEFAULT '',
                        `targetAppLabel` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_voice_commands_triggerWord` ON `voice_commands` (`triggerWord`)")
            }
        }
    }
}