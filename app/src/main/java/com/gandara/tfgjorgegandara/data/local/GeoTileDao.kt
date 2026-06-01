package com.gandara.tfgjorgegandara.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class GeoTileDao {

    @Transaction
    open suspend fun upsertSampleToTile(tileId: String, timeBucket: Long, dbValue: Double) {
        val insertedId = insertTile(
            GeoTile(
                tileId = tileId,
                timeBucket = timeBucket,
                avgDb = dbValue,
                peakDb = dbValue,
                sampleCount = 1
            )
        )

        if (insertedId == -1L) {
            updateTileStats(tileId, timeBucket, dbValue)
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTile(tile: GeoTile): Long

    @Query("""
        UPDATE geo_tiles SET
            avgDb = (avgDb * sampleCount + :dbValue) / (sampleCount + 1),
            peakDb = MAX(peakDb, :dbValue),
            sampleCount = sampleCount + 1
        WHERE tileId = :tileId AND timeBucket = :timeBucket
    """)
    abstract suspend fun updateTileStats(tileId: String, timeBucket: Long, dbValue: Double)

}
