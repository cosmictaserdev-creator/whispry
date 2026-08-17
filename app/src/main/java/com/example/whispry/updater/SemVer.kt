// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.updater

/**
 * Minimal semantic-version comparison for GitHub release tags ("v1.4.2") against
 * `BuildConfig.VERSION_NAME`. Only major.minor.patch are compared numerically; anything after a
 * hyphen (a pre-release suffix) is stripped before comparing, since Whispry doesn't currently
 * ship pre-release tags and a string-suffix ordering isn't worth the complexity yet.
 */
object SemVer {

    private fun parse(raw: String): Triple<Int, Int, Int> {
        val core = raw.removePrefix("v").substringBefore("-")
        val parts = core.split(".").map { it.toIntOrNull() ?: 0 }
        return Triple(parts.getOrElse(0) { 0 }, parts.getOrElse(1) { 0 }, parts.getOrElse(2) { 0 })
    }

    /** True if [remote] is a strictly newer version than [current]. */
    fun isNewer(remote: String, current: String): Boolean {
        val (rMajor, rMinor, rPatch) = parse(remote)
        val (cMajor, cMinor, cPatch) = parse(current)
        return when {
            rMajor != cMajor -> rMajor > cMajor
            rMinor != cMinor -> rMinor > cMinor
            else -> rPatch > cPatch
        }
    }
}
