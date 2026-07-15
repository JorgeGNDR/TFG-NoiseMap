package com.gandara.tfgjorgegandara.domain.usecase

import com.gandara.tfgjorgegandara.domain.model.HeatmapTile
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository

data class HeatmapQuery(
    val octaveIndex: Int,
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val startHour: Int? = null,
    val endHour: Int? = null
)

class GetHeatmapDataUseCase(
    private val repository: AudioRepository
) {
    suspend operator fun invoke(query: HeatmapQuery): List<HeatmapTile> {
        return repository.getHeatmapData(
            octaveIndex = query.octaveIndex,
            minLat = query.minLatitude,
            maxLat = query.maxLatitude,
            minLon = query.minLongitude,
            maxLon = query.maxLongitude,
            startTimestamp = query.startTimestamp,
            endTimestamp = query.endTimestamp,
            startHour = query.startHour,
            endHour = query.endHour
        )
    }
}
