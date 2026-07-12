package com.example.whispry.util

/**
 * Runs [action], retrying exactly once if it throws — for the transient WindowManager.addView
 * failures seen right after cold process start (overlay/pill/widget windows). [onFirstFailure]
 * lets the caller log the real exception before the retry; a second failure propagates to the
 * caller's own catch.
 */
suspend fun <T> retryOnce(onFirstFailure: suspend (Exception) -> Unit = {}, action: suspend () -> T): T {
    return try {
        action()
    } catch (e: Exception) {
        onFirstFailure(e)
        action()
    }
}
