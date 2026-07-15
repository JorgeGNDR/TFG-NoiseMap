package com.gandara.tfgjorgegandara.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FrequencyBinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBins(bins: List<FrequencyBin>)

    @Query("SELECT * FROM frequency_bins WHERE sampleId = :sampleId ORDER BY band ASC")
    suspend fun getBinsForSample(sampleId: Long): List<FrequencyBin>

    @Query("""
        SELECT 
            S.timestamp,
            S.latitude,
            S.longitude,
            F.energy
        FROM audio_samples S
        JOIN frequency_bins F ON S.id = F.sampleId
        WHERE F.band = :targetBand
        AND S.timestamp BETWEEN :startTimestamp AND :endTimestamp
        AND S.latitude BETWEEN :minLat AND :maxLat
        AND S.longitude BETWEEN :minLon AND :maxLon
    """)
    suspend fun getBandEnergiesForHeatmap(
        targetBand: Int,
        startTimestamp: Long,
        endTimestamp: Long,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<HeatmapBandRow>

    data class HeatmapBandRow(
        val timestamp: Long,
        val latitude: Double,
        val longitude: Double,
        val energy: Double
    )
}
