package com.gandara.tfgjorgegandara.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.data.ai.NoiseExplanationService
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
    private val explanationService by lazy { NoiseExplanationService() }
    
    private val _samples = MutableStateFlow<List<AudioSample>>(emptyList())
    val samples: StateFlow<List<AudioSample>> = _samples.asStateFlow()

    private val _selectedSampleDetails = MutableStateFlow<FullSampleData?>(null)
    val selectedSampleDetails: StateFlow<FullSampleData?> = _selectedSampleDetails.asStateFlow()

    private val _explainingSampleId = MutableStateFlow<Long?>(null)
    val explainingSampleId: StateFlow<Long?> = _explainingSampleId.asStateFlow()

    private val _explanationError = MutableStateFlow<String?>(null)
    val explanationError: StateFlow<String?> = _explanationError.asStateFlow()

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

    /**
     * Elimina una muestra de la base de datos.
     */
    fun deleteSample(sample: AudioSample) {
        viewModelScope.launch {
            db.audioSampleDao().deleteSample(sample)
            if (_selectedSampleDetails.value?.sample?.id == sample.id) {
                _selectedSampleDetails.value = null
            }
        }
    }

    fun explainSample(sample: AudioSample) {
        if (_explainingSampleId.value != null) return

        viewModelScope.launch {
            _explainingSampleId.value = sample.id
            _explanationError.value = null

            try {
                val currentDetails = _selectedSampleDetails.value
                val details = if (currentDetails?.sample?.id == sample.id) {
                    currentDetails
                } else {
                    val bins = db.frequencyBinDao().getBinsForSample(sample.id)
                    val classifications = db.soundClassificationDao().getClassificationsForSample(sample.id)
                    FullSampleData(sample, bins, classifications)
                }

                val explanation = explanationService.explainSample(
                    sample = details.sample,
                    bins = details.bins,
                    classifications = details.classifications
                )

                db.audioSampleDao().updateAiExplanation(sample.id, explanation)
                _selectedSampleDetails.value = details.copy(
                    sample = details.sample.copy(aiExplanation = explanation)
                )
            } catch (e: Exception) {
                val reason = e.localizedMessage ?: e.javaClass.simpleName
                _explanationError.value = "No se pudo generar la explicacion: $reason"
            } finally {
                _explainingSampleId.value = null
            }
        }
    }

    fun clearExplanationError() {
        _explanationError.value = null
    }
}
