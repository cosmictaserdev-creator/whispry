// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.updater

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface UpdateApiService {

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = UpdateConfig.REPO_OWNER,
        @Path("repo") repo: String = UpdateConfig.REPO_NAME
    ): Response<GithubReleaseDto>
}
