package com.gandara.tfgjorgegandara.data.local

import androidx.room.Dao
import androidx.room.Query

@Dao
interface GeoTileDao {

    @Query("""
        INSERT INTO geo_tiles (tileId, timeBucket, avgDb, peakDb, sampleCount)
        VALUES (:tileId, :timeBucket, :dbValue, :dbValue, 1)
        ON CONFLICT(tileId, timeBucket) DO UPDATE SET
            avgDb = (avgDb * sampleCount + EXCLUDED.avgDb) / (sampleCount + 1),
            peakDb = MAX(peakDb, EXCLUDED.peakDb),
            sampleCount = sampleCount + 1
    """)
    suspend fun upsertSampleToTile(tileId: String, timeBucket: Long, dbValue: Double)

    @Query("SELECT * FROM geo_tiles WHERE timeBucket >= :sinceBucket")
    suspend fun getRecentTiles(sinceBucket: Long): List<GeoTile>

    @Query("""
        SELECT * FROM geo_tiles 
        WHERE timeBucket >= :sinceBucket
        AND CAST(SUBSTR(tileId, 1, INSTR(tileId, '_') - 1) AS INTEGER) BETWEEN :minLatGrid AND :maxLatGrid
        AND CAST(SUBSTR(tileId, INSTR(tileId, '_') + 1) AS INTEGER) BETWEEN :minLonGrid AND :maxLonGrid
    """)
    suspend fun getTilesInBounds(
        sinceBucket: Long,
        minLatGrid: Int,
        maxLatGrid: Int,
        minLonGrid: Int,
        maxLonGrid: Int
    ): List<GeoTile>
}
