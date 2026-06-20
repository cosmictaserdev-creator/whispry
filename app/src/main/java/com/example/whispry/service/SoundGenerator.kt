package com.example.whispry.service

import kotlin.math.*

object SoundGenerator {
    
    fun generateSiriClick(): ShortArray {
        return generateTwoTone(
            freq1 = 1200f, freq2 = 1400f,
            toneDurationMs = 8, gapMs = 40,
            sampleRate = 44100
        )
    }
    
    fun generateSoftChime(): ShortArray {
        return generateChime(freq = 880f, durationMs = 180, sampleRate = 44100)
    }
    
    fun generateSoftPop(): ShortArray {
        return generateFreqSweep(
            startFreq = 600f, endFreq = 200f,
            durationMs = 30, sampleRate = 44100
        )
    }
    
    fun generateDoubleBeep(): ShortArray {
        return generateTwoTone(
            freq1 = 800f, freq2 = 900f,
            toneDurationMs = 60, gapMs = 40,
            sampleRate = 44100
        )
    }
    
    fun generateWhoosh(): ShortArray {
        val sampleRate = 44100
        val durationMs = 200
        val numSamples = (sampleRate * durationMs / 1000f).toInt()
        val samples = ShortArray(numSamples)
        val random = java.util.Random()
        
        for (i in 0 until numSamples) {
            // Filtered noise approximation
            val progress = i.toFloat() / numSamples
            val centerFreq = 400f + 1600f * progress
            val resonance = 0.95f
            
            var noise = random.nextFloat() * 2f - 1f
            // Very simple dynamic filter feel
            samples[i] = (noise * 32767 * 0.1f).toInt().toShort()
        }
        
        return applyEnvelope(samples, 50, 50, sampleRate)
    }

    private fun generateTwoTone(freq1: Float, freq2: Float, toneDurationMs: Int, gapMs: Int, sampleRate: Int): ShortArray {
        val toneSamples = (sampleRate * toneDurationMs / 1000f).toInt()
        val gapSamples = (sampleRate * gapMs / 1000f).toInt()
        val totalSamples = toneSamples * 2 + gapSamples
        val samples = ShortArray(totalSamples)
        
        for (i in 0 until toneSamples) {
            val angle = 2.0 * PI * i * freq1 / sampleRate
            samples[i] = (sin(angle) * 32767 * 0.5f).toInt().toShort()
        }
        
        for (i in 0 until toneSamples) {
            val angle = 2.0 * PI * i * freq2 / sampleRate
            samples[i + toneSamples + gapSamples] = (sin(angle) * 32767 * 0.5f).toInt().toShort()
        }
        
        return applyEnvelope(samples, 2, 6, sampleRate)
    }

    private fun generateChime(freq: Float, durationMs: Int, sampleRate: Int): ShortArray {
        val numSamples = (sampleRate * durationMs / 1000f).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * i * freq / sampleRate
            val harmonic = 2.0 * PI * i * (freq * 2) / sampleRate
            val sample = (sin(angle) + 0.5 * sin(harmonic)) / 1.5
            samples[i] = (sample * 32767 * 0.5f).toInt().toShort()
        }
        return applyEnvelope(samples, 20, 160, sampleRate)
    }

    private fun generateFreqSweep(startFreq: Float, endFreq: Float, durationMs: Int, sampleRate: Int): ShortArray {
        val numSamples = (sampleRate * durationMs / 1000f).toInt()
        val samples = ShortArray(numSamples)
        var currentPhase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            currentPhase += 2.0 * PI * freq / sampleRate
            samples[i] = (sin(currentPhase) * 32767 * 0.5f).toInt().toShort()
        }
        return applyEnvelope(samples, 2, durationMs - 2, sampleRate)
    }
    
    private fun applyEnvelope(samples: ShortArray, attackMs: Int, decayMs: Int, sampleRate: Int): ShortArray {
        val attackSamples = (sampleRate * attackMs / 1000f).toInt()
        val decaySamples = (sampleRate * decayMs / 1000f).toInt()
        return samples.mapIndexed { i, sample ->
            val envelope = when {
                i < attackSamples -> i.toFloat() / attackSamples
                i > samples.size - decaySamples -> {
                    val decayPos = i - (samples.size - decaySamples)
                    1f - (decayPos.toFloat() / decaySamples)
                }
                else -> 1f
            }
            (sample * envelope).toInt().toShort()
        }.toShortArray()
    }
}
