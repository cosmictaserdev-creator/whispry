// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class TranscriptionResponseDto(
    @SerializedName("text")
    val text: String
)