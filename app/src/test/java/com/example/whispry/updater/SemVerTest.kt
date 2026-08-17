package com.example.whispry.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    @Test
    fun `newer patch version is detected`() {
        assertTrue(SemVer.isNewer("v1.2.0", "1.1.9"))
        assertFalse(SemVer.isNewer("v1.1.9", "1.2.0"))
    }

    @Test
    fun `equal versions are not newer`() {
        assertFalse(SemVer.isNewer("1.0.0", "1.0.0"))
        assertFalse(SemVer.isNewer("v1.0.0", "1.0.0"))
    }

    @Test
    fun `missing patch component defaults to zero`() {
        assertFalse(SemVer.isNewer("v1.0", "1.0.0"))
        assertTrue(SemVer.isNewer("v1.1", "1.0.5"))
    }

    @Test
    fun `pre-release suffix is ignored for ordering`() {
        assertTrue(SemVer.isNewer("v2.0.0-beta", "1.9.9"))
    }
}
