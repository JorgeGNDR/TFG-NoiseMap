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

    /**
     * Obtiene la energía promedio agrupada por zonas espaciales para una banda específica.
     * Esto permite el filtrado por frecuencia en el mapa de calor.
     */
    @Query("""
        SELECT 
            (CAST(S.latitude * 10000 AS INTEGER) || '_' || CAST(S.longitude * 10000 AS INTEGER)) as tileId,
            AVG(F.energy) as avgEnergy,
            AVG(S.latitude) as lat,
            AVG(S.longitude) as lon
        FROM audio_samples S
        JOIN frequency_bins F ON S.id = F.sampleId
        WHERE F.band = :targetBand
        AND S.latitude BETWEEN :minLat AND :maxLat
        AND S.longitude BETWEEN :minLon AND :maxLon
        GROUP BY tileId
    """)
    suspend fun getEnergyByTileAndBand(
        targetBand: Int,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<TileEnergyResult>

    /**
     * Obtiene energía agregada por perfiles (Low, Mid, High).
     */
    @Query("""
        SELECT 
            (CAST(S.latitude * 10000 AS INTEGER) || '_' || CAST(S.longitude * 10000 AS INTEGER)) as tileId,
            AVG(F.energy) as avgEnergy,
            AVG(S.latitude) as lat,
            AVG(S.longitude) as lon
        FROM audio_samples S
        JOIN frequency_bins F ON S.id = F.sampleId
        WHERE F.band BETWEEN :minBand AND :maxBand
        AND S.latitude BETWEEN :minLat AND :maxLat
        AND S.longitude BETWEEN :minLon AND :maxLon
        GROUP BY tileId
    """)
    suspend fun getEnergyByTileAndBandRange(
        minBand: Int,
        maxBand: Int,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<TileEnergyResult>

    data class TileEnergyResult(
        val tileId: String,
        val avgEnergy: Double,
        val lat: Double,
        val lon: Double
    )
}
