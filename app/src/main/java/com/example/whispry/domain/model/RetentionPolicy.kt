// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.domain.model

enum class RetentionPolicy(val displayName: String, val days: Int?, val description: String) {
    FOREVER("Keep forever", null, "Transcripts never deleted automatically"),
    ONE_WEEK("1 week", 7, "Older transcripts deleted daily"),
    TWO_WEEKS("2 weeks", 14, "Older transcripts deleted daily"),
    ONE_MONTH("1 month", 30, "Older transcripts deleted daily"),
    THREE_MONTHS("3 months", 90, "Older transcripts deleted daily"),
    SIX_MONTHS("6 months", 180, "Older transcripts deleted daily")
}
