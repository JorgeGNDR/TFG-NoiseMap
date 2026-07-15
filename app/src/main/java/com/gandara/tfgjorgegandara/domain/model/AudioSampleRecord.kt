package com.gandara.tfgjorgegandara.domain.model

data class AudioSampleRecord(
    val id: Long = 0,
    val sessionId: Long? = null,
    val timestamp: Long,
    val durationMs: Long = 3000L,
    val latitude: Double,
    val longitude: Double,
    val avgDb: Float,
    val peakDb: Float,
    val dominantFreq: Float = 0f,
    val weighting: String,
    val calibrationOffset: Float = 90f,
    val aiExplanation: String? = null
)
