// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import kotlin.math.*

object MFCCExtractor {
    private const val SAMPLE_RATE = 16000
    private const val NUM_COEFFICIENTS = 13
    private const val FRAME_SIZE = 512
    private const val HOP_SIZE = 256
    private const val NUM_MEL_FILTERS = 26
    
    fun extract(audioSamples: FloatArray): FloatArray {
        // Step 1: Pre-emphasis filter
        val emphasized = preEmphasis(audioSamples, 0.97f)
        
        // Step 2: Frame the signal
        val frames = frameSignal(emphasized, FRAME_SIZE, HOP_SIZE)
        
        // Step 3: Mel processing per frame
        if (frames.isEmpty()) {
            return FloatArray(NUM_COEFFICIENTS)
        }
        
        val melFilters = createMelFilterbank(NUM_MEL_FILTERS, FRAME_SIZE, SAMPLE_RATE)
        val allMfccs = mutableListOf<FloatArray>()
        
        for (frame in frames) {
            // Apply Hamming window
            val windowed = applyHammingWindow(frame)
            
            // FFT
            val spectrum = fft(windowed)
            
            // Mel energies
            val melEnergies = FloatArray(NUM_MEL_FILTERS)
            for (i in 0 until NUM_MEL_FILTERS) {
                var energy = 0.0
                for (j in 0 until FRAME_SIZE / 2) {
                    energy += melFilters[i][j] * spectrum[j]
                }
                melEnergies[i] = ln(energy.coerceAtLeast(1e-10)).toFloat()
            }
            
            // DCT
            val mfcc = dct(melEnergies, NUM_COEFFICIENTS)
            allMfccs.add(mfcc)
        }
        
        // Step 8: Average across all frames → single feature vector
        return averageFrames(allMfccs)
    }
    
    private fun preEmphasis(signal: FloatArray, coefficient: Float): FloatArray {
        val result = FloatArray(signal.size)
        result[0] = signal[0]
        for (i in 1 until signal.size) {
            result[i] = signal[i] - coefficient * signal[i - 1]
        }
        return result
    }

    private fun frameSignal(signal: FloatArray, frameSize: Int, hopSize: Int): List<FloatArray> {
        val frames = mutableListOf<FloatArray>()
        var start = 0
        while (start + frameSize <= signal.size) {
            frames.add(signal.copyOfRange(start, start + frameSize))
            start += hopSize
        }
        return frames
    }
    
    private fun applyHammingWindow(frame: FloatArray): FloatArray {
        val result = FloatArray(frame.size)
        for (i in frame.indices) {
            result[i] = (frame[i] * (0.54 - 0.46 * cos(2 * PI * i / (frame.size - 1)))).toFloat()
        }
        return result
    }

    private fun fft(frame: FloatArray): FloatArray {
        val n = frame.size
        val real = frame.copyOf()
        val imag = FloatArray(n)
        
        fftIterative(real, imag)
        
        // Return power spectrum (first half)
        val spectrum = FloatArray(n / 2)
        for (i in 0 until n / 2) {
            spectrum[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        return spectrum
    }

    // Iterative Radix-2 FFT
    private fun fftIterative(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tempR = real[i]; real[i] = real[j]; real[j] = tempR
                val tempI = imag[i]; imag[i] = imag[j]; imag[j] = tempI
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }
        
        var len = 2
        while (n >= len) {
            val ang = 2.0 * PI / len * -1.0
            val wlenR = cos(ang).toFloat()
            val wlenI = sin(ang).toFloat()
            var i = 0
            while (n > i) {
                var wR = 1f
                var wI = 0f
                for (k in 0 until len / 2) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + len / 2] * wR - imag[i + k + len / 2] * wI
                    val vI = real[i + k + len / 2] * wR + imag[i + k + len / 2] * wI
                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + len / 2] = uR - vR
                    imag[i + k + len / 2] = uI - vI
                    val nextWR = wR * wlenR - wI * wlenI
                    wI = wR * wlenI + wI * wlenR
                    wR = nextWR
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun createMelFilterbank(numFilters: Int, frameSize: Int, sampleRate: Int): Array<FloatArray> {
        val minFreq = 0.0
        val maxFreq = sampleRate / 2.0
        val minMel = freqToMel(minFreq)
        val maxMel = freqToMel(maxFreq)
        
        val melPoints = FloatArray(numFilters + 2)
        for (i in 0 until numFilters + 2) {
            melPoints[i] = melToFreq(minMel + i * (maxMel - minMel) / (numFilters + 1)).toFloat()
        }
        
        val binPoints = IntArray(numFilters + 2)
        for (i in 0 until numFilters + 2) {
            binPoints[i] = (floor((frameSize + 1) * melPoints[i] / sampleRate)).toInt()
        }
        
        val filters = Array(numFilters) { FloatArray(frameSize / 2) }
        for (m in 1..numFilters) {
            for (k in binPoints[m - 1] until binPoints[m]) {
                filters[m - 1][k] = (k - binPoints[m - 1]).toFloat() / (binPoints[m] - binPoints[m - 1])
            }
            for (k in binPoints[m] until binPoints[m + 1]) {
                filters[m - 1][k] = (binPoints[m + 1] - k).toFloat() / (binPoints[m + 1] - binPoints[m])
            }
        }
        return filters
    }

    private fun freqToMel(freq: Double) = 2595.0 * log10(1.0 + freq / 700.0)
    private fun melToFreq(mel: Double) = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)

    private fun dct(melEnergies: FloatArray, numCoefficients: Int): FloatArray {
        val n = melEnergies.size
        val result = FloatArray(numCoefficients)
        for (i in 0 until numCoefficients) {
            var sum = 0.0
            for (j in 0 until n) {
                sum += melEnergies[j] * cos(PI * i * (j + 0.5) / n)
            }
            result[i] = sum.toFloat()
        }
        return result
    }

    private fun averageFrames(allMfccs: List<FloatArray>): FloatArray {
        if (allMfccs.isEmpty()) return FloatArray(NUM_COEFFICIENTS)
        val result = FloatArray(NUM_COEFFICIENTS)
        for (i in 0 until NUM_COEFFICIENTS) {
            var sum = 0.0
            for (mfcc in allMfccs) {
                sum += mfcc[i]
            }
            result[i] = (sum / allMfccs.size).toFloat()
        }
        return result
    }
}
