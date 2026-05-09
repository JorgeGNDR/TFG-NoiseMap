package com.gandara.tfgjorgegandara.data.local

import androidx.room.Entity

@Entity(
    tableName = "geo_tiles",
    primaryKeys = ["tileId", "timeBucket"]
)
data class GeoTile(
    val tileId: String,       // ID basado en rejilla: "lat_lon"
    val timeBucket: Long,     // Agrupación temporal (ej: por hora)
    val avgDb: Double,
    val peakDb: Double,
    val sampleCount: Int
)
