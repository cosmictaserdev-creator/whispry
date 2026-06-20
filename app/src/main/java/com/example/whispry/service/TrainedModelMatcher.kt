package com.example.whispry.service

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class TrainedModelMatcher @Inject constructor() {
    private var storedFingerprint: FloatArray? = null
    private val threshold = 0.78f
    
    fun train(samples: List<FloatArray>) {
        val fingerprints = samples.map { MFCCExtractor.extract(it) }
        storedFingerprint = averageVectors(fingerprints)
    }
    
    fun matches(audioSamples: FloatArray): Boolean {
        val fingerprint = storedFingerprint ?: return false
        val incoming = MFCCExtractor.extract(audioSamples)
        val similarity = cosineSimilarity(fingerprint, incoming)
        return similarity >= threshold
    }
    
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
    }
    
    private fun averageVectors(vectors: List<FloatArray>): FloatArray {
        if (vectors.isEmpty()) return FloatArray(13)
        val size = vectors[0].size
        val result = FloatArray(size)
        for (i in 0 until size) {
            var sum = 0.0
            for (v in vectors) {
                sum += v[i]
            }
            result[i] = (sum / vectors.size).toFloat()
        }
        return result
    }

    fun getFingerprintString(): String? {
        return storedFingerprint?.joinToString(",")
    }

    fun loadFromPrefsString(saved: String) {
        try {
            storedFingerprint = saved.split(",").map { it.toFloat() }.toFloatArray()
        } catch (e: Exception) {
            storedFingerprint = null
        }
    }
}
