package com.gandara.tfgjorgegandara.presentation.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.di.AppContainer
import com.gandara.tfgjorgegandara.domain.usecase.GetHeatmapDataUseCase
import com.gandara.tfgjorgegandara.domain.usecase.HeatmapQuery
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val getHeatmapData: GetHeatmapDataUseCase = AppContainer.getHeatmapData(application)
    private var updateJob: Job? = null

    private val _heatmapPoints = MutableStateFlow<List<HeatMapPoint>>(emptyList())
    val heatmapPoints: StateFlow<List<HeatMapPoint>> = _heatmapPoints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter())
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    private val _hourFilter = MutableStateFlow(HourFilter())
    val hourFilter: StateFlow<HourFilter> = _hourFilter.asStateFlow()

    private val _selectedBandIndex = MutableStateFlow(-1)
    val selectedBandIndex: StateFlow<Int> = _selectedBandIndex.asStateFlow()

    private var currentBounds: MapBounds? = null

    companion object {
        private const val MIN_DB = 20.0
        private const val MAX_DB = 90.0
        private const val MIN_VISIBLE_INTENSITY = 0.18
    }

    data class MapBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )

    fun setDateFilterMode(mode: DateFilterMode) {
        _dateFilter.value = _dateFilter.value.copy(mode = mode)
        refreshHeatmap()
    }

    fun setSingleDate(dayStartMillis: Long) {
        _dateFilter.value = _dateFilter.value.copy(
            mode = DateFilterMode.SINGLE_DAY,
            singleDayMillis = dayStartMillis.startOfDay()
        )
        refreshHeatmap()
    }

    fun setRangeStartDate(dayStartMillis: Long) {
        val current = _dateFilter.value
        val normalizedStart = dayStartMillis.startOfDay()
        val normalizedEnd = maxOf(current.rangeEndMillis.startOfDay(), normalizedStart)
        _dateFilter.value = current.copy(
            mode = DateFilterMode.DATE_RANGE,
            rangeStartMillis = normalizedStart,
            rangeEndMillis = normalizedEnd
        )
        refreshHeatmap()
    }

    fun setRangeEndDate(dayStartMillis: Long) {
        val current = _dateFilter.value
        val normalizedEnd = dayStartMillis.startOfDay()
        val normalizedStart = minOf(current.rangeStartMillis.startOfDay(), normalizedEnd)
        _dateFilter.value = current.copy(
            mode = DateFilterMode.DATE_RANGE,
            rangeStartMillis = normalizedStart,
            rangeEndMillis = normalizedEnd
        )
        refreshHeatmap()
    }

    fun setAllDayFilter(enabled: Boolean) {
        _hourFilter.value = _hourFilter.value.copy(allDay = enabled)
        refreshHeatmap()
    }

    fun setStartHour(hour: Int) {
        val current = _hourFilter.value
        val normalizedStartHour = hour.coerceIn(0, 23)
        val normalizedEndHour = if (normalizedStartHour >= current.endHour) {
            (normalizedStartHour + 1).coerceAtMost(24)
        } else {
            current.endHour
        }

        _hourFilter.value = current.copy(
            startHour = normalizedStartHour,
            endHour = normalizedEndHour
        )
        refreshHeatmap()
    }

    fun setEndHour(hour: Int) {
        val current = _hourFilter.value
        val normalizedEndHour = hour.coerceIn(1, 24)
        val normalizedStartHour = if (normalizedEndHour <= current.startHour) {
            (normalizedEndHour - 1).coerceAtLeast(0)
        } else {
            current.startHour
        }

        _hourFilter.value = current.copy(
            startHour = normalizedStartHour,
            endHour = normalizedEndHour
        )
        refreshHeatmap()
    }

    fun setFrequencyBandIndex(index: Int) {
        _selectedBandIndex.value = index
        refreshHeatmap()
    }

    private fun refreshHeatmap() {
        currentBounds?.let { updateHeatMap(it.minLat, it.maxLat, it.minLon, it.maxLon) }
    }

    fun updateHeatMap(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) {
        currentBounds = MapBounds(minLat, maxLat, minLon, maxLon)
        updateJob?.cancel()

        updateJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val bandIndex = _selectedBandIndex.value
                val dateFilter = _dateFilter.value
                val hourFilter = _hourFilter.value

                val tiles = getHeatmapData(
                    HeatmapQuery(
                        octaveIndex = bandIndex,
                        minLatitude = minLat,
                        maxLatitude = maxLat,
                        minLongitude = minLon,
                        maxLongitude = maxLon,
                        startTimestamp = dateFilter.startTimestamp,
                        endTimestamp = dateFilter.endTimestamp,
                        startHour = hourFilter.activeStartHour,
                        endHour = hourFilter.activeEndHour
                    )
                )

                _heatmapPoints.value = tiles.map { tile ->
                    HeatMapPoint(
                        geoPoint = GeoPoint(tile.lat, tile.lon),
                        intensity = normalizeDb(tile.avgDb, bandIndex == -1),
                        rawDb = tile.avgDb
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "No se pudieron cargar las mediciones del mapa"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun normalizeDb(db: Double, isGlobal: Boolean): Double {
        val min = if (isGlobal) MIN_DB else 0.0
        val max = if (isGlobal) MAX_DB else 80.0
        val normalized = ((db - min) / (max - min)).coerceIn(0.0, 1.0)
        return if (normalized > 0.0) normalized.coerceAtLeast(MIN_VISIBLE_INTENSITY) else MIN_VISIBLE_INTENSITY
    }

    data class HeatMapPoint(val geoPoint: GeoPoint, val intensity: Double, val rawDb: Double)

    data class GeoPoint(val latitude: Double, val longitude: Double)

    enum class DateFilterMode(val label: String) {
        LAST_24_HOURS("Últimas 24 h"),
        LAST_7_DAYS("Últimos 7 días"),
        LAST_30_DAYS("Últimos 30 días"),
        SINGLE_DAY("Fecha concreta"),
        DATE_RANGE("Rango de fechas"),
        ALL_HISTORY("Todo el historial")
    }

    data class DateFilter(
        val mode: DateFilterMode = DateFilterMode.LAST_24_HOURS,
        val singleDayMillis: Long = todayStartMillis(),
        val rangeStartMillis: Long = todayStartMillis(),
        val rangeEndMillis: Long = todayStartMillis()
    ) {
        val label: String
            get() = when (mode) {
                DateFilterMode.SINGLE_DAY -> formatDate(singleDayMillis)
                DateFilterMode.DATE_RANGE -> "${formatDate(rangeStartMillis)} - ${formatDate(rangeEndMillis)}"
                else -> mode.label
            }

        val startTimestamp: Long
            get() {
                val now = System.currentTimeMillis()
                return when (mode) {
                    DateFilterMode.LAST_24_HOURS -> now - TimeUnit.HOURS.toMillis(24)
                    DateFilterMode.LAST_7_DAYS -> now - TimeUnit.DAYS.toMillis(7)
                    DateFilterMode.LAST_30_DAYS -> now - TimeUnit.DAYS.toMillis(30)
                    DateFilterMode.SINGLE_DAY -> singleDayMillis.startOfDay()
                    DateFilterMode.DATE_RANGE -> rangeStartMillis.startOfDay()
                    DateFilterMode.ALL_HISTORY -> 0L
                }
            }

        val endTimestamp: Long
            get() {
                return when (mode) {
                    DateFilterMode.SINGLE_DAY -> singleDayMillis.endOfDay()
                    DateFilterMode.DATE_RANGE -> rangeEndMillis.endOfDay()
                    else -> System.currentTimeMillis()
                }
            }
    }

    data class HourFilter(
        val allDay: Boolean = true,
        val startHour: Int = 0,
        val endHour: Int = 24
    ) {
        val activeStartHour: Int?
            get() = if (allDay) null else startHour

        val activeEndHour: Int?
            get() = if (allDay) null else endHour

        val label: String
            get() = if (allDay) {
                "Todo el día"
            } else {
                "${startHour.toHourLabel()}-${endHour.toHourLabel()}"
            }
    }
}

private fun todayStartMillis(): Long = System.currentTimeMillis().startOfDay()

private fun Long.startOfDay(): Long {
    return Calendar.getInstance().apply {
        timeInMillis = this@startOfDay
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun Long.endOfDay(): Long {
    return Calendar.getInstance().apply {
        timeInMillis = this@endOfDay
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp)
}

private fun Int.toHourLabel(): String = if (this == 24) "24:00" else "%02d:00".format(this)
