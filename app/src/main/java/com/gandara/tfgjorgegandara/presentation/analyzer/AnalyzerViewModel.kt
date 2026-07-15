package com.gandara.tfgjorgegandara.presentation.analyzer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.di.AppContainer
import com.gandara.tfgjorgegandara.domain.audio.AudioCaptureSource
import com.gandara.tfgjorgegandara.domain.audio.DecibelMath
import com.gandara.tfgjorgegandara.domain.audio.FFTCalculator
import com.gandara.tfgjorgegandara.domain.audio.SoundClassifier
import com.gandara.tfgjorgegandara.domain.audio.SpectrumWeighting
import com.gandara.tfgjorgegandara.domain.audio.ThirdOctaveCalculator
import com.gandara.tfgjorgegandara.domain.location.GeoLocation
import com.gandara.tfgjorgegandara.domain.model.AudioMeasurement
import com.gandara.tfgjorgegandara.domain.model.WeightingType
import com.gandara.tfgjorgegandara.domain.settings.SettingsRepository
import com.gandara.tfgjorgegandara.domain.usecase.CreateMeasurementSessionUseCase
import com.gandara.tfgjorgegandara.domain.usecase.FinishMeasurementSessionUseCase
import com.gandara.tfgjorgegandara.domain.usecase.SaveAudioSampleUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

data class AnalyzerState(
    val isPaused: Boolean = false,
    val isCapturing: Boolean = false,
    val captureProgress: Float = 0f,
    val decibels: Double = 0.0,
    val dbA: Double = 0.0,
    val dbC: Double = 0.0,
    val dbZ: Double = 0.0,
    val selectedWeighting: WeightingType = WeightingType.A,
    val peak: Double = 0.0,
    val avg: Double = 0.0,
    val spectrum: FloatArray = FloatArray(0),
    val peakHoldSpectrum: FloatArray = FloatArray(0),
    val offset: Float = 90f,
    val detectedSound: String = "",
    val isSaving: Boolean = false,
    val captureFeedback: String? = null
)

class AnalyzerViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val SAMPLE_RATE = 44100
        const val UI_UPDATE_INTERVAL_MS = 50L
        const val CLASSIFICATION_INTERVAL = 10
        const val SEGMENT_DURATION_MS = 5000L
        const val MIN_SEGMENT_DURATION_MS = 3000L
        const val MAX_SESSION_DURATION_MS = 120_000L
        const val PEAK_DECAY_DB = 0.6f
    }

    private val settings: SettingsRepository = AppContainer.settings(application)
    private var currentBufferSize = settings.state.value.spectrumBufferSize
    private var audioManager: AudioCaptureSource = AppContainer.audioCapture(currentBufferSize)
    private var fftCalculator = FFTCalculator(currentBufferSize)
    private val classifier: SoundClassifier = AppContainer.soundClassifier(application)

    private val saveAudioSample: SaveAudioSampleUseCase = AppContainer.saveAudioSample(application)
    private val createMeasurementSession: CreateMeasurementSessionUseCase =
        AppContainer.createMeasurementSession(application)
    private val finishMeasurementSession: FinishMeasurementSessionUseCase =
        AppContainer.finishMeasurementSession(application)

    private val _uiState = MutableStateFlow(AnalyzerState())
    val uiState: StateFlow<AnalyzerState> = _uiState.asStateFlow()

    private var lastUiUpdateTime = 0L
    private var classificationCounter = 0

    private var sumOfSquarePressures = 0.0
    private var dbCount = 0
    private var internalPeak = 0.0
    private var currentPeakHold = FloatArray(0)

    private var isCaptureActive = false
    private var sessionId: Long? = null
    private var sessionStartTime = 0L
    private var segmentStartTime = 0L
    private var sessionJob: Job? = null
    private var latestLocation: GeoLocation? = null

    private var captureSpectrumEnergySum: DoubleArray? = null
    private var captureDbEnergySum = 0.0
    private var captureCount = 0
    private var captureMaxDb = -100.0
    private val captureLabels = mutableMapOf<String, Float>()

    init {
        _uiState.update { it.copy(offset = settings.state.value.calibrationOffset) }
        startAnalyzing()

        viewModelScope.launch {
            settings.state.collect { currentSettings ->
                applyAnalyzerSettings(
                    currentSettings.spectrumBufferSize,
                    currentSettings.calibrationOffset
                )
            }
        }
    }

    private fun applyAnalyzerSettings(bufferSize: Int, offset: Float) {
        val bufferChanged = bufferSize != currentBufferSize
        _uiState.update { it.copy(offset = offset) }

        if (bufferChanged) {
            currentBufferSize = bufferSize
            audioManager.stop()
            audioManager = AppContainer.audioCapture(currentBufferSize)
            fftCalculator = FFTCalculator(currentBufferSize)
            currentPeakHold = FloatArray(0)
            startAnalyzing()
        }
    }

    fun updateCurrentLocation(location: GeoLocation?) {
        if (location != null) latestLocation = location
    }

    fun startAnalyzing() {
        audioManager.start { audioBuffer ->
            val results = fftCalculator.calculateWeightings(audioBuffer, SAMPLE_RATE, _uiState.value.offset)
            classifier.offerAudio(audioBuffer, SAMPLE_RATE)
            val currentDb = when (_uiState.value.selectedWeighting) {
                WeightingType.A -> results.a
                WeightingType.C -> results.c
                WeightingType.Z -> results.z
            }

            classificationCounter++
            if (classificationCounter >= CLASSIFICATION_INTERVAL) {
                classificationCounter = 0
                viewModelScope.launch(Dispatchers.Default) {
                    val classification = classifier.classifyLatest()

                    if (isCaptureActive && classification.probability != null) {
                        synchronized(captureLabels) {
                            val currentProb = captureLabels.getOrDefault(classification.label, 0f)
                            captureLabels[classification.label] = max(currentProb, classification.probability)
                        }
                    }

                    _uiState.update { it.copy(detectedSound = classification.displayText) }
                }
            }

            if (isCaptureActive) {
                processCaptureData(currentDb, results.spectrum)
            }

            if (!_uiState.value.isPaused) {
                updateVisuals(currentDb, results)
            }
        }
    }

    private fun processCaptureData(currentDb: Double, spectrum: FloatArray) {
        captureDbEnergySum += DecibelMath.dbToEnergy(currentDb)
        captureCount++
        captureMaxDb = max(captureMaxDb, currentDb)

        if (captureSpectrumEnergySum == null) {
            captureSpectrumEnergySum = DoubleArray(spectrum.size)
        }

        captureSpectrumEnergySum?.let { energySum ->
            for (i in spectrum.indices) {
                energySum[i] += DecibelMath.dbToEnergy(spectrum[i].toDouble())
            }
        }
    }

    private fun updateVisuals(currentDb: Double, results: FFTCalculator.WeightedResults) {
        val weightedSpectrum = SpectrumWeighting.applyVisualWeighting(
            spectrum = results.spectrum,
            weightingType = _uiState.value.selectedWeighting,
            fftCalculator = fftCalculator,
            sampleRate = SAMPLE_RATE,
            fftSize = currentBufferSize
        )

        if (currentPeakHold.size != weightedSpectrum.size) {
            currentPeakHold = weightedSpectrum.clone()
        } else {
            for (i in weightedSpectrum.indices) {
                currentPeakHold[i] = max(weightedSpectrum[i], currentPeakHold[i] - PEAK_DECAY_DB)
                if (currentPeakHold[i] < -20f) currentPeakHold[i] = -20f
            }
        }

        dbCount++
        sumOfSquarePressures += 10.0.pow(currentDb / 10.0)
        internalPeak = max(internalPeak, currentDb)

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUiUpdateTime >= UI_UPDATE_INTERVAL_MS) {
            lastUiUpdateTime = currentTime
            val trueAvgDb = 10.0 * log10(sumOfSquarePressures / dbCount)
            _uiState.update {
                it.copy(
                    decibels = currentDb,
                    dbA = results.a,
                    dbC = results.c,
                    dbZ = results.z,
                    peak = internalPeak,
                    avg = trueAvgDb,
                    spectrum = weightedSpectrum,
                    peakHoldSpectrum = currentPeakHold.clone()
                )
            }
        }
    }

    fun toggleCaptureSession(location: GeoLocation? = latestLocation) {
        if (_uiState.value.isCapturing) {
            stopCaptureSession()
        } else {
            startCaptureSession(location)
        }
    }

    fun startCaptureSession(location: GeoLocation? = latestLocation) {
        if (_uiState.value.isCapturing || _uiState.value.isSaving) return
        val initialLocation = location ?: run {
            _uiState.update {
                it.copy(captureFeedback = "Se necesita una ubicación válida para guardar la muestra")
            }
            return
        }

        latestLocation = initialLocation
        Log.d("AnalyzerViewModel", "Sesión de medición iniciada")

        sessionJob = viewModelScope.launch {
            val startTimestamp = System.currentTimeMillis()
            val newSessionId = createMeasurementSession(startTimestamp).getOrElse { error ->
                _uiState.update {
                    it.copy(captureFeedback = "No se pudo iniciar la sesión: ${error.localizedMessage ?: "error desconocido"}")
                }
                return@launch
            }

            sessionId = newSessionId
            sessionStartTime = startTimestamp
            segmentStartTime = startTimestamp
            resetCaptureAccumulators()
            isCaptureActive = true
            _uiState.update { it.copy(isCapturing = true, captureProgress = 0f, captureFeedback = null) }

            while (isCaptureActive) {
                val now = System.currentTimeMillis()
                val sessionElapsed = now - sessionStartTime
                val segmentElapsed = now - segmentStartTime

                _uiState.update {
                    it.copy(captureProgress = (sessionElapsed.toFloat() / MAX_SESSION_DURATION_MS).coerceIn(0f, 1f))
                }

                if (segmentElapsed >= SEGMENT_DURATION_MS) {
                    persistCurrentSegment(segmentElapsed)
                    segmentStartTime = System.currentTimeMillis()
                    resetCaptureAccumulators()
                }

                if (sessionElapsed >= MAX_SESSION_DURATION_MS) {
                    stopCaptureSession("Sesión finalizada al alcanzar el límite de 2 minutos")
                    break
                }

                delay(100)
            }
        }
    }

    fun stopCaptureSession(message: String = "Sesión guardada correctamente") {
        if (!_uiState.value.isCapturing) return
        isCaptureActive = false
        val lastSegmentDuration = System.currentTimeMillis() - segmentStartTime
        val currentSessionId = sessionId
        val startedAt = sessionStartTime

        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = false, captureProgress = 0f, isSaving = true) }

            if (lastSegmentDuration >= MIN_SEGMENT_DURATION_MS && captureCount > 0) {
                persistCurrentSegment(lastSegmentDuration)
            }

            if (currentSessionId != null && startedAt > 0L) {
                val endTimestamp = System.currentTimeMillis()
                finishMeasurementSession(currentSessionId, endTimestamp, endTimestamp - startedAt)
            }

            sessionId = null
            sessionStartTime = 0L
            segmentStartTime = 0L
            resetCaptureAccumulators()
            _uiState.update { it.copy(isSaving = false, captureFeedback = message) }
            delay(3_500)
            _uiState.update { it.copy(captureFeedback = null) }
        }
    }

    private suspend fun persistCurrentSegment(durationMs: Long) {
        val location = latestLocation ?: return
        val currentSessionId = sessionId ?: return
        if (captureCount <= 0 || captureDbEnergySum <= 0.0) return

        val avgDb = DecibelMath.energyToDb(captureDbEnergySum / captureCount)
        val finalSpectrum = captureSpectrumEnergySum?.map { energySum ->
            if (energySum > 0.0) {
                DecibelMath.energyToDb(energySum / captureCount, -20.0).toFloat()
            } else {
                -20f
            }
        }?.toFloatArray() ?: return

        val dominantFreq = ThirdOctaveCalculator.dominantFrequency(finalSpectrum, SAMPLE_RATE, currentBufferSize)
        val thirdOctaveBands = ThirdOctaveCalculator.calculateBands(finalSpectrum, SAMPLE_RATE, currentBufferSize)
        val labels = synchronized(captureLabels) { captureLabels.toMap() }
        val state = _uiState.value
        val segmentTimestamp = segmentStartTime

        val result = withContext(Dispatchers.IO) {
            saveAudioSample(
                AudioMeasurement(
                    sessionId = currentSessionId,
                    timestamp = segmentTimestamp,
                    durationMs = durationMs,
                    avgDb = avgDb.toFloat(),
                    peakDb = captureMaxDb.toFloat(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    spectralEnergy = thirdOctaveBands,
                    detectedSounds = labels,
                    dominantFrequency = dominantFreq,
                    weighting = state.selectedWeighting,
                    calibrationOffset = state.offset
                )
            )
        }

        if (result.isFailure) {
            _uiState.update {
                it.copy(
                    captureFeedback = "No se pudo guardar un segmento: " +
                        (result.exceptionOrNull()?.localizedMessage ?: "error desconocido")
                )
            }
        }
    }

    private fun resetCaptureAccumulators() {
        captureDbEnergySum = 0.0
        captureCount = 0
        captureMaxDb = -100.0
        captureSpectrumEnergySum = null
        synchronized(captureLabels) {
            captureLabels.clear()
        }
    }

    fun setWeighting(type: WeightingType) {
        _uiState.update { it.copy(selectedWeighting = type) }
        resetPeak()
        resetAvg()
    }

    fun resetCurrentDb() {
        _uiState.update { it.copy(decibels = 0.0) }
    }

    fun resetPeak() {
        internalPeak = 0.0
        _uiState.update { it.copy(peak = 0.0) }
    }

    fun resetAvg() {
        sumOfSquarePressures = 0.0
        dbCount = 0
        _uiState.update { it.copy(avg = 0.0) }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    override fun onCleared() {
        super.onCleared()
        sessionJob?.cancel()
        audioManager.stop()
        classifier.close()
    }
}
