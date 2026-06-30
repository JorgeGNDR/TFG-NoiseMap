package com.gandara.tfgjorgegandara.presentation.analyzer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.di.AppContainer
import com.gandara.tfgjorgegandara.domain.model.AudioMeasurement
import com.gandara.tfgjorgegandara.domain.model.WeightingType
import com.gandara.tfgjorgegandara.domain.location.GeoLocation
import com.gandara.tfgjorgegandara.domain.usecase.SaveAudioSampleUseCase
import com.gandara.tfgjorgegandara.domain.audio.AudioCaptureSource
import com.gandara.tfgjorgegandara.domain.audio.FFTCalculator
import com.gandara.tfgjorgegandara.domain.audio.DecibelMath
import com.gandara.tfgjorgegandara.domain.audio.SpectrumWeighting
import com.gandara.tfgjorgegandara.domain.audio.ThirdOctaveCalculator
import com.gandara.tfgjorgegandara.domain.audio.SoundClassifier
import com.gandara.tfgjorgegandara.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Estado que representa los datos de análisis sonoro en tiempo real y el progreso de captura.
 */
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
    }

    private val settings: SettingsRepository = AppContainer.settings(application)
    private var currentBufferSize = settings.state.value.spectrumBufferSize
    private var audioManager: AudioCaptureSource = AppContainer.audioCapture(currentBufferSize)
    private var fftCalculator = FFTCalculator(currentBufferSize)
    private val classifier: SoundClassifier = AppContainer.soundClassifier(application)
    
    private val saveAudioSample: SaveAudioSampleUseCase
    private var recordingJob: Job? = null

    private val _uiState = MutableStateFlow(AnalyzerState())
    val uiState: StateFlow<AnalyzerState> = _uiState.asStateFlow()

    private var lastUiUpdateTime = 0L
    private val UI_UPDATE_INTERVAL_MS = 50L
    private var classificationCounter = 0
    private val CLASSIFICATION_INTERVAL = 10 // ejecución periódica de clasificación (~500ms)

    private var sumOfSquarePressures = 0.0
    private var dbCount = 0
    private var internalPeak = 0.0
    private var currentPeakHold = FloatArray(0)
    private val PEAK_DECAY_DB = 0.6f

    // Acumuladores para sesiones de captura de 3 segundos
    private var isCaptureActive = false
    private var captureStartTime = 0L
    private val CAPTURE_DURATION_MS = 3000L
    // Los niveles en decibelios son logarítmicos: durante la captura se acumula
    // energía lineal y solo se vuelve a dB al finalizar la sesión.
    private var captureSpectrumEnergySum: DoubleArray? = null
    private var captureDbEnergySum = 0.0
    private var captureCount = 0
    private var captureMaxDb = -100.0
    
    // Almacén para YAMNet durante los 3 segundos
    private val captureYAMNetLabels = mutableMapOf<String, Float>()

    init {
        currentBufferSize = settings.state.value.spectrumBufferSize
        audioManager = AppContainer.audioCapture(currentBufferSize)
        fftCalculator = FFTCalculator(currentBufferSize)
        _uiState.update { it.copy(offset = settings.state.value.calibrationOffset) }

        saveAudioSample = AppContainer.saveAudioSample(application)
        
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

    /**
     * Inicia el bucle de captura y analisis FFT de la senal de audio.
     */
    fun startAnalyzing() {
        audioManager.start { audioBuffer ->
            val results = fftCalculator.calculateWeightings(audioBuffer, SAMPLE_RATE, _uiState.value.offset)
            classifier.offerAudio(audioBuffer, SAMPLE_RATE)
            val currentDb = when (_uiState.value.selectedWeighting) {
                WeightingType.A -> results.a
                WeightingType.C -> results.c
                WeightingType.Z -> results.z
            }

            // Gestión de la clasificación por redes neuronales (YAMNet) - CAMINO A
            classificationCounter++
            if (classificationCounter >= CLASSIFICATION_INTERVAL) {
                classificationCounter = 0
                viewModelScope.launch(Dispatchers.Default) {
                    // YAMNet consume la última ventana remuestreada del flujo compartido.
                    val classification = classifier.classifyLatest()
                    
                    if (isCaptureActive && classification.probability != null) {
                        synchronized(captureYAMNetLabels) {
                            val currentProb = captureYAMNetLabels.getOrDefault(classification.label, 0f)
                            captureYAMNetLabels[classification.label] = max(
                                currentProb,
                                classification.probability
                            )
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

    /**
     * Procesa y acumula los datos durante una sesión de captura controlada.
     */
    private fun processCaptureData(currentDb: Double, spectrum: FloatArray) {
        captureDbEnergySum += DecibelMath.dbToEnergy(currentDb)
        captureCount++
        captureMaxDb = max(captureMaxDb, currentDb)

        if (captureSpectrumEnergySum == null) {
            captureSpectrumEnergySum = DoubleArray(spectrum.size)
        }

        val energySum = captureSpectrumEnergySum
        if (energySum != null) {
            for (i in spectrum.indices) {
                energySum[i] += DecibelMath.dbToEnergy(spectrum[i].toDouble())
            }
        }

        // No es necesaria la clasificación energética manual por bandas básicas aquí
        // Ya que ahora calculamos tercios de octava completos al finalizar
    }

    /**
     * Actualiza el estado de la UI con los valores calculados de intensidad y espectro.
     */
    private fun updateVisuals(currentDb: Double, results: FFTCalculator.WeightedResults) {
        val weightedSpectrum = SpectrumWeighting.applyVisualWeighting(
            spectrum = results.spectrum,
            weightingType = _uiState.value.selectedWeighting,
            fftCalculator = fftCalculator,
            sampleRate = SAMPLE_RATE,
            fftSize = currentBufferSize
        )

        // Lógica de "Peak Hold" para la visualización del espectro
        if (currentPeakHold.size != weightedSpectrum.size) currentPeakHold = weightedSpectrum.clone()
        else {
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
            _uiState.update { it.copy(
                decibels = currentDb,
                dbA = results.a, dbC = results.c, dbZ = results.z,
                peak = internalPeak,
                avg = trueAvgDb,
                spectrum = weightedSpectrum,
                peakHoldSpectrum = currentPeakHold.clone()
            )}
        }
    }

    /**
     * Inicia una sesión de captura de datos de duración determinada (3 s).
     */
    fun startCaptureSession(location: GeoLocation? = null) {
        if (_uiState.value.isCapturing || _uiState.value.isSaving) return
        val captureLocation = location ?: run {
            _uiState.update {
                it.copy(captureFeedback = "Se necesita una ubicación válida para guardar la muestra")
            }
            return
        }
        Log.d("AnalyzerViewModel", "Captura iniciada. Lat: ${captureLocation.latitude}")
        
        viewModelScope.launch {
            _uiState.update {
                it.copy(isCapturing = true, captureProgress = 0f, captureFeedback = null)
            }
            isCaptureActive = true
            captureStartTime = System.currentTimeMillis()
            resetCaptureAccumulators()

            while (System.currentTimeMillis() - captureStartTime < CAPTURE_DURATION_MS) {
                val elapsed = System.currentTimeMillis() - captureStartTime
                _uiState.update { it.copy(captureProgress = elapsed.toFloat() / CAPTURE_DURATION_MS) }
                delay(100)
            }
            finalizeCapture(captureLocation)
        }
    }

    /**
     * Finaliza la sesión de captura y persiste los resultados en la base de datos.
     */
    private fun finalizeCapture(location: GeoLocation) {
        isCaptureActive = false
        val avgDb = if (captureCount > 0 && captureDbEnergySum > 0.0) {
            DecibelMath.energyToDb(captureDbEnergySum / captureCount)
        } else {
            0.0
        }
        val finalSpectrum = if (captureCount > 0) {
            captureSpectrumEnergySum?.map { energySum ->
                if (energySum > 0.0) {
                    DecibelMath.energyToDb(energySum / captureCount, -20.0).toFloat()
                } else {
                    -20f
                }
            }?.toFloatArray() ?: FloatArray(0)
        } else {
            FloatArray(0)
        }
        
        val dominantFreq = ThirdOctaveCalculator.dominantFrequency(finalSpectrum, SAMPLE_RATE, currentBufferSize)

        val thirdOctaveBands = ThirdOctaveCalculator.calculateBands(finalSpectrum, SAMPLE_RATE, currentBufferSize)

        _uiState.update { it.copy(isCapturing = false, captureProgress = 0f, isSaving = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val labels = synchronized(captureYAMNetLabels) { captureYAMNetLabels.toMap() }

            val result = saveAudioSample(
                AudioMeasurement(
                    avgDb = avgDb.toFloat(),
                    peakDb = captureMaxDb.toFloat(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    spectralEnergy = thirdOctaveBands,
                    detectedSounds = labels,
                    dominantFrequency = dominantFreq,
                    weighting = state.selectedWeighting
                )
            )

            _uiState.update {
                it.copy(
                    isSaving = false,
                    captureFeedback = result.fold(
                        onSuccess = { "Muestra guardada correctamente" },
                        onFailure = { error ->
                            "No se pudo guardar la muestra: " +
                                (error.localizedMessage ?: "error desconocido")
                        }
                    )
                )
            }
            delay(3_500)
            _uiState.update { it.copy(captureFeedback = null) }
        }
    }

    private fun resetCaptureAccumulators() {
        captureDbEnergySum = 0.0
        captureCount = 0
        captureMaxDb = -100.0
        captureSpectrumEnergySum = null
        synchronized(captureYAMNetLabels) {
            captureYAMNetLabels.clear()
        }
    }

    fun setWeighting(type: WeightingType) {
        _uiState.update { it.copy(selectedWeighting = type) }
        resetPeak(); resetAvg()
    }

    fun resetCurrentDb() { _uiState.update { it.copy(decibels = 0.0) } }
    fun resetPeak() { internalPeak = 0.0; _uiState.update { it.copy(peak = 0.0) } }
    fun resetAvg() { sumOfSquarePressures = 0.0; dbCount = 0; _uiState.update { it.copy(avg = 0.0) } }
    fun togglePause() { _uiState.update { it.copy(isPaused = !it.isPaused) } }

    override fun onCleared() {
        super.onCleared()
        audioManager.stop()
        classifier.close()
        recordingJob?.cancel()
    }
}
