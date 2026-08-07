package com.gandara.tfgjorgegandara.presentation.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.di.AppContainer
import com.gandara.tfgjorgegandara.domain.model.AudioSampleRecord
import com.gandara.tfgjorgegandara.domain.model.FullAudioSample
import com.gandara.tfgjorgegandara.domain.model.HistoryContent
import com.gandara.tfgjorgegandara.domain.model.MeasurementSessionHistory
import com.gandara.tfgjorgegandara.domain.usecase.HistoryUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val useCases: HistoryUseCases = AppContainer.historyUseCases(application)

    private val _samples = MutableStateFlow<List<AudioSampleRecord>>(emptyList())
    val samples: StateFlow<List<AudioSampleRecord>> = _samples.asStateFlow()

    private val _historyContent = MutableStateFlow(HistoryContent())
    val historyContent: StateFlow<HistoryContent> = _historyContent.asStateFlow()

    private val _selectedSampleDetails = MutableStateFlow<FullAudioSample?>(null)
    val selectedSampleDetails: StateFlow<FullAudioSample?> = _selectedSampleDetails.asStateFlow()

    private val _explainingSampleId = MutableStateFlow<Long?>(null)
    val explainingSampleId: StateFlow<Long?> = _explainingSampleId.asStateFlow()

    private val _explanationError = MutableStateFlow<String?>(null)
    val explanationError: StateFlow<String?> = _explanationError.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedSampleIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSampleIds: StateFlow<Set<Long>> = _selectedSampleIds.asStateFlow()

    private val _isDeletingSamples = MutableStateFlow(false)
    val isDeletingSamples: StateFlow<Boolean> = _isDeletingSamples.asStateFlow()

    private val _deletionError = MutableStateFlow<String?>(null)
    val deletionError: StateFlow<String?> = _deletionError.asStateFlow()

    init {
        loadSamples()
    }

    private fun loadSamples() {
        viewModelScope.launch {
            useCases.observeHistory().collect { history ->
                _historyContent.value = history
                _samples.value = history.allSamples
                val availableIds = history.allSamples.mapTo(mutableSetOf()) { it.id }
                _selectedSampleIds.value = _selectedSampleIds.value.intersect(availableIds)
            }
        }
    }

    fun loadSampleDetails(sample: AudioSampleRecord) {
        if (_selectedSampleDetails.value?.sample?.id == sample.id) {
            _selectedSampleDetails.value = null
            return

        }

        viewModelScope.launch {
            _selectedSampleDetails.value = useCases.getSampleDetails(sample)
        }
    }

    fun deleteSample(sample: AudioSampleRecord) {
        viewModelScope.launch {
            _deletionError.value = null
            try {
                useCases.deleteSample(sample)
                if (_selectedSampleDetails.value?.sample?.id == sample.id) {
                    _selectedSampleDetails.value = null
                }
            } catch (e: Exception) {
                _deletionError.value = "No se pudo eliminar la muestra"
            }
        }
    }

    fun enterSelectionMode() {
        _selectedSampleDetails.value = null
        _isSelectionMode.value = true
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedSampleIds.value = emptySet()
    }

    fun toggleSampleSelection(sampleId: Long) {
        if (!_isSelectionMode.value) {
            enterSelectionMode()
        }

        _selectedSampleIds.value = _selectedSampleIds.value.toMutableSet().apply {
            if (!add(sampleId)) remove(sampleId)
        }
    }

    fun selectAllSamples() {
        _selectedSampleIds.value = _samples.value.mapTo(mutableSetOf()) { it.id }
    }

    fun deleteSelectedSamples() {
        if (_isDeletingSamples.value) return

        val selectedIds = _selectedSampleIds.value
        val selectedSamples = _samples.value.filter { it.id in selectedIds }
        if (selectedSamples.isEmpty()) return

        viewModelScope.launch {
            _isDeletingSamples.value = true
            _deletionError.value = null
            try {
                useCases.deleteSamples(selectedSamples)
                val expandedSampleId = _selectedSampleDetails.value?.sample?.id
                if (expandedSampleId != null && expandedSampleId in selectedIds) {
                    _selectedSampleDetails.value = null
                }
                exitSelectionMode()
            } catch (e: Exception) {
                _deletionError.value = "No se pudieron eliminar las muestras seleccionadas"
            } finally {
                _isDeletingSamples.value = false
            }
        }
    }

    fun deleteSession(session: MeasurementSessionHistory) {
        viewModelScope.launch {
            _deletionError.value = null
            try {
                useCases.deleteSession(session.session.id)
                val selectedDetails = _selectedSampleDetails.value
                if (selectedDetails?.sample?.sessionId == session.session.id) {
                    _selectedSampleDetails.value = null
                }
                val deletedIds = session.samples.mapTo(mutableSetOf()) { it.id }
                _selectedSampleIds.value = _selectedSampleIds.value - deletedIds
            } catch (e: Exception) {
                _deletionError.value = "No se pudo eliminar la sesión"
            }
        }
    }

    fun explainSample(sample: AudioSampleRecord) {
        if (_explainingSampleId.value != null) return

        viewModelScope.launch {
            _explainingSampleId.value = sample.id
            _explanationError.value = null

            try {
                val currentDetails = _selectedSampleDetails.value
                val details = if (currentDetails?.sample?.id == sample.id) {
                    currentDetails
                } else {
                    useCases.getSampleDetails(sample)
                }

                val explanation = useCases.explainSample(details)

                _selectedSampleDetails.value = details.copy(
                    sample = details.sample.copy(aiExplanation = explanation)
                )
            } catch (e: Exception) {
                val reason = e.localizedMessage ?: e.javaClass.simpleName
                _explanationError.value = "No se pudo generar la explicación: $reason"
            } finally {
                _explainingSampleId.value = null
            }
        }
    }

    fun clearExplanationError() {
        _explanationError.value = null
    }

    fun clearDeletionError() {
        _deletionError.value = null
    }
}
