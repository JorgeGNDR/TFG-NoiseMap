package com.gandara.tfgjorgegandara.domain.model

data class AudioSampleRecord(
    val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val avgDb: Float,
    val peakDb: Float,
    val dominantFreq: Float = 0f,
    val weighting: String,
    val aiExplanation: String? = null
)
