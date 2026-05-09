package com.gandara.tfgjorgegandara.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.data.local.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Agregador de datos que representa una muestra de audio con todos sus detalles asociados.
 */
data class FullSampleData(
    val sample: AudioSample,
    val bins: List<FrequencyBin>,
    val classifications: List<SoundClassification>
)

/**
 * ViewModel que gestiona la recuperación y presentación del histórico de mediciones acústicas.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    
    private val _samples = MutableStateFlow<List<AudioSample>>(emptyList())
    val samples: StateFlow<List<AudioSample>> = _samples.asStateFlow()

    private val _selectedSampleDetails = MutableStateFlow<FullSampleData?>(null)
    val selectedSampleDetails: StateFlow<FullSampleData?> = _selectedSampleDetails.asStateFlow()

    init {
        loadSamples()
    }

    /**
     * Recupera el listado completo de muestras almacenadas en la base de datos local.
     */
    private fun loadSamples() {
        viewModelScope.launch {
            db.audioSampleDao().getAllSamples().collect {
                _samples.value = it
            }
        }
    }

    /**
     * Carga el detalle completo (espectro y etiquetas IA) para una muestra específica.
     */
    fun loadSampleDetails(sample: AudioSample) {
        viewModelScope.launch {
            val bins = db.frequencyBinDao().getBinsForSample(sample.id)
            val classifications = db.soundClassificationDao().getClassificationsForSample(sample.id)
            
            _selectedSampleDetails.value = FullSampleData(sample, bins, classifications)
        }
    }
}
