// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class ErrorResponseDto(
    @SerializedName("error")
    val error: ErrorDetailDto?
)

data class ErrorDetailDto(
    @SerializedName("message")
    val message: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("code")
    val code: String?
)