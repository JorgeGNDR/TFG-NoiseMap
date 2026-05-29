package com.gandara.tfgjorgegandara.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_samples",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["latitude", "longitude"])
    ]
)
data class AudioSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val avgDb: Float,
    val peakDb: Float,
    val dominantFreq: Float = 0f,
    val weighting: String, // "A", "C" o "Z"
    val aiExplanation: String? = null
)
