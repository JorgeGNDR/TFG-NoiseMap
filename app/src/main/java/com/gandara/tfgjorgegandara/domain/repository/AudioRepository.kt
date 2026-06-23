package com.gandara.tfgjorgegandara.domain.repository

import com.gandara.tfgjorgegandara.domain.model.HeatmapTile

interface AudioRepository {
    suspend fun saveCompleteAudioSample(
        avgDb: Float,
        peakDb: Float,
        latitude: Double,
        longitude: Double,
        spectralEnergy: FloatArray,
        labels: Map<String, Float>,
        dominantFreq: Float = 0f,
        weighting: String = "A"
    ): Result<Long>

    suspend fun getHeatmapData(
        octaveIndex: Int,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        sinceHoursAgo: Int = 24
    ): List<HeatmapTile>
}
