package com.gandara.tfgjorgegandara.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_sessions")
data class MeasurementSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val durationMs: Long = 0L
)
