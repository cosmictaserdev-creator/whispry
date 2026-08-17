// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.updater

/** A release ready to show/install, already picked apart from the raw GitHub DTO. */
data class UpdateRelease(
    val tagName: String,
    val name: String,
    val notes: String,
    val downloadUrl: String,
    val assetName: String
)
