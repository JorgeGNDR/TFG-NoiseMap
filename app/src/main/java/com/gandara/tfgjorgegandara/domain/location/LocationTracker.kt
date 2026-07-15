package com.gandara.tfgjorgegandara.domain.location

import kotlinx.coroutines.flow.Flow

data class GeoLocation(
    val latitude: Double,
    val longitude: Double
)

interface LocationTracker {
    fun updates(intervalInSeconds: Long = 5): Flow<GeoLocation?>
    fun getCurrentLocation(onResult: (GeoLocation?) -> Unit)
    fun getLastLocation(onResult: (GeoLocation?) -> Unit)
}
