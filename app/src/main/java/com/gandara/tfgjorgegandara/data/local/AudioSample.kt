package com.gandara.tfgjorgegandara.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_samples",
    foreignKeys = [
        ForeignKey(
            entity = MeasurementSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["latitude", "longitude"]),
        Index(value = ["sessionId"])
    ]
)
data class AudioSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long? = null,
    val timestamp: Long,
    val durationMs: Long,
    val latitude: Double,
    val longitude: Double,
    val avgDb: Float,
    val peakDb: Float,
    val dominantFreq: Float = 0f,
    val weighting: String, // "A", "C" o "Z"
    val calibrationOffset: Float = 90f,
    val aiExplanation: String? = null
)
