// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int,
    @SerializedName("temperature") val temperature: Float,
    @SerializedName("reasoning_effort") val reasoningEffort: String? = null
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatCompletionResponse(
    @SerializedName("choices") val choices: List<ChatChoice>
)

data class ChatChoice(
    @SerializedName("message") val message: ChatMessage
)
