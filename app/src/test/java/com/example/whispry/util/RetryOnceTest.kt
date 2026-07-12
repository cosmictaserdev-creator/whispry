package com.example.whispry.util

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryOnceTest {

    @Test
    fun `succeeds on first try without retrying`() = runTest {
        var calls = 0
        val result = retryOnce { calls++; "ok" }

        assertEquals("ok", result)
        assertEquals(1, calls)
    }

    @Test
    fun `retries exactly once after a failure, then succeeds`() = runTest {
        var calls = 0
        var loggedFailure = false

        val result = retryOnce(onFirstFailure = { loggedFailure = true }) {
            calls++
            if (calls == 1) throw RuntimeException("transient") else "ok"
        }

        assertEquals("ok", result)
        assertEquals(2, calls)
        assertTrue(loggedFailure)
    }

    @Test
    fun `propagates the second failure instead of retrying forever`() = runTest {
        var calls = 0

        try {
            retryOnce {
                calls++
                throw RuntimeException("still broken")
            }
            org.junit.Assert.fail("expected an exception")
        } catch (e: RuntimeException) {
            assertEquals("still broken", e.message)
        }

        assertEquals(2, calls)
    }
}
