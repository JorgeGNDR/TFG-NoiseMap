package com.gandara.tfgjorgegandara.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.data.repository.RepositoryProvider
import com.gandara.tfgjorgegandara.domain.model.AudioSampleRecord
import com.gandara.tfgjorgegandara.domain.model.FullAudioSample
import com.gandara.tfgjorgegandara.domain.repository.HistoryRepository
import com.gandara.tfgjorgegandara.domain.repository.NoiseExplanationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository: HistoryRepository = RepositoryProvider.historyRepository(application)
    private val explanationRepository: NoiseExplanationRepository = RepositoryProvider.noiseExplanationRepository()

    private val _samples = MutableStateFlow<List<AudioSampleRecord>>(emptyList())
    val samples: StateFlow<List<AudioSampleRecord>> = _samples.asStateFlow()

    private val _selectedSampleDetails = MutableStateFlow<FullAudioSample?>(null)
    val selectedSampleDetails: StateFlow<FullAudioSample?> = _selectedSampleDetails.asStateFlow()

    private val _explainingSampleId = MutableStateFlow<Long?>(null)
    val explainingSampleId: StateFlow<Long?> = _explainingSampleId.asStateFlow()

    private val _explanationError = MutableStateFlow<String?>(null)
    val explanationError: StateFlow<String?> = _explanationError.asStateFlow()

    init {
        loadSamples()
    }

    private fun loadSamples() {
        viewModelScope.launch {
            historyRepository.observeSamples().collect { samples ->
                _samples.value = samples
            }
        }
    }

    fun loadSampleDetails(sample: AudioSampleRecord) {
        if (_selectedSampleDetails.value?.sample?.id == sample.id) {
            _selectedSampleDetails.value = null
            return

        }

        viewModelScope.launch {
            _selectedSampleDetails.value = historyRepository.getSampleDetails(sample)
        }
    }

    fun deleteSample(sample: AudioSampleRecord) {
        viewModelScope.launch {
            historyRepository.deleteSample(sample)
            if (_selectedSampleDetails.value?.sample?.id == sample.id) {
                _selectedSampleDetails.value = null
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
                    historyRepository.getSampleDetails(sample)
                }

                val explanation = explanationRepository.explainSample(details)
                historyRepository.updateAiExplanation(sample.id, explanation)

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
