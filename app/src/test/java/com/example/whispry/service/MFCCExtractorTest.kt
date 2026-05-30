package com.example.whispry.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MFCCExtractorTest {

    @Test
    fun `test extraction consistency`() {
        val samples = FloatArray(16000) { Math.sin(2.0 * Math.PI * it * 440.0 / 16000).toFloat() }
        val mfcc1 = MFCCExtractor.extract(samples)
        val mfcc2 = MFCCExtractor.extract(samples)
        
        assertEquals(13, mfcc1.size)
        for (i in mfcc1.indices) {
            assertEquals(mfcc1[i], mfcc2[i], 0.0001f)
        }
    }

    @Test
    fun `test different signals produce different mfccs`() {
        val samples1 = FloatArray(16000) { Math.sin(2.0 * Math.PI * it * 440.0 / 16000).toFloat() }
        val samples2 = FloatArray(16000) { Math.sin(2.0 * Math.PI * it * 880.0 / 16000).toFloat() }
        
        val mfcc1 = MFCCExtractor.extract(samples1)
        val mfcc2 = MFCCExtractor.extract(samples2)
        
        var diff = 0f
        for (i in mfcc1.indices) {
            diff += Math.abs(mfcc1[i] - mfcc2[i])
        }
        assertNotEquals(0f, diff, 0.01f)
    }
}
