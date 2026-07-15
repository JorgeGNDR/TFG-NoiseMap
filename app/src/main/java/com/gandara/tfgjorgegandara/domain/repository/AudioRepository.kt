package com.gandara.tfgjorgegandara.domain.repository

import com.gandara.tfgjorgegandara.domain.model.HeatmapTile

interface AudioRepository {
    suspend fun createMeasurementSession(startTimestamp: Long): Result<Long>

    suspend fun finishMeasurementSession(
        sessionId: Long,
        endTimestamp: Long,
        durationMs: Long
    ): Result<Unit>

    suspend fun saveCompleteAudioSample(
        sessionId: Long?,
        timestamp: Long,
        durationMs: Long,
        avgDb: Float,
        peakDb: Float,
        latitude: Double,
        longitude: Double,
        spectralEnergy: FloatArray,
        labels: Map<String, Float>,
        dominantFreq: Float = 0f,
        weighting: String = "A",
        calibrationOffset: Float
    ): Result<Long>

    suspend fun getHeatmapData(
        octaveIndex: Int,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        startTimestamp: Long,
        endTimestamp: Long,
        startHour: Int? = null,
        endHour: Int? = null
    ): List<HeatmapTile>
}
