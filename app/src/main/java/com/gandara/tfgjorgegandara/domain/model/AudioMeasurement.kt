package com.gandara.tfgjorgegandara.domain.model

/**
 * Medición acústica completa preparada para su persistencia.
 */
data class AudioMeasurement(
    val avgDb: Float,
    val peakDb: Float,
    val latitude: Double,
    val longitude: Double,
    val spectralEnergy: FloatArray,
    val detectedSounds: Map<String, Float>,
    val dominantFrequency: Float,
    val weighting: WeightingType
)
