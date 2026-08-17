// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.updater

import com.google.gson.annotations.SerializedName

/** Subset of the GitHub "get latest release" response this app actually reads. */
data class GithubReleaseDto(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("prerelease") val prerelease: Boolean = false,
    @SerializedName("assets") val assets: List<GithubReleaseAssetDto> = emptyList()
)

data class GithubReleaseAssetDto(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)
