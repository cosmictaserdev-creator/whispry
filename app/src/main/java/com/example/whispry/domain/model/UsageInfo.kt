// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.model

data class UsageInfo(
    val requestsUsed: Int,
    val wordsUsed: Int,
    val dailyLimit: Int = 200_000
) {
    val requestsPercent: Float
        get() = (requestsUsed.toFloat() / dailyLimit).coerceIn(0f, 1f)

    val wordsPercent: Float
        get() = ((wordsUsed.toFloat() / dailyLimit) * 5f).coerceIn(0f, 1f)

    val isLimitReached: Boolean
        get() = requestsUsed >= dailyLimit

    val requestsRemaining: Int
        get() = (dailyLimit - requestsUsed).coerceAtLeast(0)
}
