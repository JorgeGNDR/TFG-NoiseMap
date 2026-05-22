package com.gandara.tfgjorgegandara.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioSampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: AudioSample)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSampleAndGetId(sample: AudioSample): Long

    @Query("SELECT * FROM audio_samples WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getSamplesByTimeRange(startTime: Long, endTime: Long): Flow<List<AudioSample>>

    @Query("""
        SELECT * FROM audio_samples 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLon AND :maxLon
    """)
    fun getSamplesInBoundingBox(
        minLat: Double, 
        maxLat: Double, 
        minLon: Double, 
        maxLon: Double
    ): Flow<List<AudioSample>>

    @Query("SELECT * FROM audio_samples ORDER BY timestamp DESC")
    fun getAllSamples(): Flow<List<AudioSample>>

    @androidx.room.Delete
    suspend fun deleteSample(sample: AudioSample)
}
