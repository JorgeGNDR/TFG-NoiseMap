package com.gandara.tfgjorgegandara.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.data.local.AppDatabase
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/**
 * ViewModel que gestiona la lógica de negocio de la pantalla del mapa,
 * incluyendo la carga de datos para el mapa de calor y la gestión de filtros.
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AudioRepository
    private var updateJob: Job? = null

    // Puntos procesados para mostrar en el mapa de calor
    private val _heatmapPoints = MutableStateFlow<List<HeatMapPoint>>(emptyList())
    val heatmapPoints: StateFlow<List<HeatMapPoint>> = _heatmapPoints.asStateFlow()

    // Estado de carga para mostrar indicadores en la UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Filtro de rango temporal (en horas)
    private val _selectedTimeRange = MutableStateFlow(24)
    val selectedTimeRange: StateFlow<Int> = _selectedTimeRange.asStateFlow()

    // Filtro de banda de frecuencia seleccionada
    private val _selectedFrequencyBand = MutableStateFlow("Global")
    val selectedFrequencyBand: StateFlow<String> = _selectedFrequencyBand.asStateFlow()

    // Límites actuales del mapa para refrescos dinámicos
    private var currentBounds: MapBounds? = null

    companion object {
        private const val MIN_DB = 30.0
        private const val MAX_DB = 100.0
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AudioRepository(
            db.audioSampleDao(),
            db.geoTileDao(),
            db.frequencyBinDao(),
            db.soundClassificationDao()
        )
    }

    /**
     * Define los límites geográficos del área visible del mapa.
     */
    data class MapBounds(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)

    /**
     * Actualiza el rango temporal de las muestras a mostrar.
     */
    fun setTimeRange(hours: Int) {
        _selectedTimeRange.value = hours
        refreshHeatmap()
    }

    /**
     * Actualiza el filtro de frecuencia para el mapa de calor.
     */
    fun setFrequencyBand(band: String) {
        _selectedFrequencyBand.value = band
        refreshHeatmap()
    }

    /**
     * Refresca los datos del mapa utilizando los filtros y límites actuales.
     */
    private fun refreshHeatmap() {
        currentBounds?.let { updateHeatMap(it.minLat, it.maxLat, it.minLon, it.maxLon) }
    }

    /**
     * Recupera y procesa los datos de ruido dentro de los límites geográficos proporcionados.
     */
    fun updateHeatMap(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) {
        currentBounds = MapBounds(minLat, maxLat, minLon, maxLon)
        updateJob?.cancel()
        
        updateJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val bandName = _selectedFrequencyBand.value
                val hours = _selectedTimeRange.value

                // Selección de fuente de datos según el filtro de frecuencia
                val tiles = when (bandName) {
                    "Bajos" -> repository.getHeatmapByFrequency(10, minLat, maxLat, minLon, maxLon)
                    "Medios" -> repository.getHeatmapByFrequency(100, minLat, maxLat, minLon, maxLon)
                    "Agudos" -> repository.getHeatmapByFrequency(500, minLat, maxLat, minLon, maxLon)
                    else -> repository.getTilesInBounds(minLat, maxLat, minLon, maxLon, sinceHoursAgo = hours)
                }
                
                // Mapeo a modelo de vista con normalización de intensidad
                val points = tiles.map { tile ->
                    HeatMapPoint(
                        geoPoint = GeoPoint(tile.lat, tile.lon),
                        intensity = normalizeDb(tile.avgDb)
                    )
                }
                _heatmapPoints.value = points
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Normaliza los valores de decibelios a un rango entre 0.0 y 1.0 para la representación visual.
     */
    private fun normalizeDb(db: Double): Double {
        return ((db - MIN_DB) / (MAX_DB - MIN_DB)).coerceIn(0.0, 1.0)
    }

    /**
     * Representación de un punto de intensidad sonora en el mapa.
     */
    data class HeatMapPoint(val geoPoint: GeoPoint, val intensity: Double)
}
