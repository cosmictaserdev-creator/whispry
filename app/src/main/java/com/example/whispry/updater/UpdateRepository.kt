// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.updater

import com.example.whispry.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val release: UpdateRelease) : UpdateCheckResult
}

@Singleton
class UpdateRepository @Inject constructor(
    private val api: UpdateApiService
) {
    suspend fun checkForUpdate(): Result<UpdateCheckResult> {
        val response = try {
            api.getLatestRelease()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (!response.isSuccessful) {
            return Result.failure(Exception("GitHub returned HTTP ${response.code()}"))
        }
        val dto = response.body() ?: return Result.failure(Exception("Empty release response"))
        val apkAsset = dto.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return Result.failure(Exception("Latest release has no APK attached"))

        return if (SemVer.isNewer(dto.tagName, BuildConfig.VERSION_NAME)) {
            Result.success(
                UpdateCheckResult.Available(
                    UpdateRelease(
                        tagName = dto.tagName,
                        name = dto.name?.ifBlank { dto.tagName } ?: dto.tagName,
                        notes = dto.body.orEmpty(),
                        downloadUrl = apkAsset.browserDownloadUrl,
                        assetName = apkAsset.name
                    )
                )
            )
        } else {
            Result.success(UpdateCheckResult.UpToDate)
        }
    }
}
