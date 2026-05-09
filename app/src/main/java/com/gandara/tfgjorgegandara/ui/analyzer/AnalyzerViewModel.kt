package com.gandara.tfgjorgegandara.ui.analyzer

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gandara.tfgjorgegandara.data.local.AppDatabase
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository
import com.gandara.tfgjorgegandara.dsp.AudioCaptureManager
import com.gandara.tfgjorgegandara.dsp.FFTCalculator
import com.gandara.tfgjorgegandara.ml.SoundClassifierManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Tipos de ponderación frecuencial aplicables al análisis sonoro.
 */
enum class WeightingType { A, C, Z }

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
    val isAutoSaving: Boolean = false
)

/**
 * ViewModel que gestiona la lógica de procesamiento digital de señales (DSP) e inteligencia artificial.
 */
class AnalyzerViewModel(application: Application) : AndroidViewModel(application) {
    private val audioManager = AudioCaptureManager()
    private val fftCalculator = FFTCalculator(4096)
    private val classifierManager = SoundClassifierManager(application)
    
    private val repository: AudioRepository
    private var recordingJob: Job? = null

    private val _uiState = MutableStateFlow(AnalyzerState())
    val uiState: StateFlow<AnalyzerState> = _uiState.asStateFlow()

    private var lastUiUpdateTime = 0L
    private val UI_UPDATE_INTERVAL_MS = 50L
    private var classificationCounter = 0
    private val CLASSIFICATION_INTERVAL = 10 // Ejecución periódica de clasificación (~500ms)

    private var sumOfSquarePressures = 0.0
    private var dbCount = 0
    private var internalPeak = 0.0
    private var currentPeakHold = FloatArray(0)
    private val DECAY_FACTOR = 0.98f

    // Acumuladores para sesiones de captura de 5 segundos
    private var isCaptureActive = false
    private var captureStartTime = 0L
    private val CAPTURE_DURATION_MS = 5000L
    private var captureSpectrumSum: FloatArray? = null
    private var captureDbSum = 0.0
    private var captureCount = 0
    private var captureMaxDb = -100.0
    private var energyLow = 0.0
    private var energyMid = 0.0
    private var energyHigh = 0.0

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AudioRepository(
            db.audioSampleDao(), 
            db.geoTileDao(), 
            db.frequencyBinDao(),
            db.soundClassificationDao()
        )
        
        classifierManager.start()
        startAnalyzing()
    }

    /**
     * Inicia el bucle de captura y análisis FFT de la señal de audio.
     */
    fun startAnalyzing() {
        audioManager.startRecording { audioBuffer, _ ->
            val results = fftCalculator.calculateWeightings(audioBuffer, 44100, _uiState.value.offset)
            val currentDb = when (_uiState.value.selectedWeighting) {
                WeightingType.A -> results.a
                WeightingType.C -> results.c
                WeightingType.Z -> results.z
            }

            // Gestión de la clasificación por redes neuronales (YAMNet)
            classificationCounter++
            if (classificationCounter >= CLASSIFICATION_INTERVAL) {
                classificationCounter = 0
                val bufferCopy = audioBuffer.clone()
                viewModelScope.launch(Dispatchers.Default) {
                    val floatBuffer = FloatArray(bufferCopy.size) { i -> bufferCopy[i] / 32768.0f }
                    val resampled = resampleTo16kHz(floatBuffer)
                    val label = classifierManager.classify(resampled)
                    _uiState.update { it.copy(detectedSound = label) }
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
        captureDbSum += currentDb
        captureCount++
        captureMaxDb = max(captureMaxDb, currentDb)

        if (captureSpectrumSum == null) {
            captureSpectrumSum = spectrum.clone()
        } else {
            for (i in spectrum.indices) {
                captureSpectrumSum!![i] += spectrum[i]
            }
        }

        // Clasificación energética por bandas básicas
        val binSize = 44100.0 / 4096
        spectrum.forEachIndexed { i, dbValue ->
            val freq = i * binSize
            val energy = 10.0.pow(dbValue / 10.0)
            when {
                freq < 250 -> energyLow += energy
                freq < 4000 -> energyMid += energy
                else -> energyHigh += energy
            }
        }
    }

    /**
     * Actualiza el estado de la UI con los valores calculados de intensidad y espectro.
     */
    private fun updateVisuals(currentDb: Double, results: FFTCalculator.WeightedResults) {
        val binSize = 44100.0 / 4096
        val weightedSpectrum = results.spectrum.clone()

        // Aplicación de ponderación al espectro visual si no es Z (lineal)
        if (_uiState.value.selectedWeighting != WeightingType.Z) {
            for (i in weightedSpectrum.indices) {
                val weight = when (_uiState.value.selectedWeighting) {
                    WeightingType.A -> fftCalculator.getAWeight(i * binSize)
                    WeightingType.C -> fftCalculator.getCWeight(i * binSize)
                    else -> 0.0
                }
                weightedSpectrum[i] = (weightedSpectrum[i] + weight).toFloat()
            }
        }

        // Lógica de "Peak Hold" para la visualización del espectro
        if (currentPeakHold.size != weightedSpectrum.size) currentPeakHold = weightedSpectrum.clone()
        else {
            for (i in weightedSpectrum.indices) {
                currentPeakHold[i] = max(weightedSpectrum[i], currentPeakHold[i] * DECAY_FACTOR)
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
     * Inicia una sesión de captura de datos de duración determinada (5s).
     */
    fun startCaptureSession(location: Location? = null) {
        if (_uiState.value.isCapturing) return
        Log.d("AnalyzerViewModel", "Captura iniciada. Lat: ${location?.latitude}")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true, captureProgress = 0f) }
            isCaptureActive = true
            captureStartTime = System.currentTimeMillis()
            resetCaptureAccumulators()

            while (System.currentTimeMillis() - captureStartTime < CAPTURE_DURATION_MS) {
                val elapsed = System.currentTimeMillis() - captureStartTime
                _uiState.update { it.copy(captureProgress = elapsed.toFloat() / CAPTURE_DURATION_MS) }
                delay(100)
            }
            finalizeCapture(location)
        }
    }

    /**
     * Finaliza la sesión de captura y persiste los resultados en la base de datos.
     */
    private fun finalizeCapture(location: Location?) {
        isCaptureActive = false
        val avgDb = captureDbSum / captureCount

        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val labels = if (state.detectedSound.isNotEmpty()) {
                mapOf(state.detectedSound to 1.0f)
            } else emptyMap()

            repository.saveCompleteAudioSample(
                avgDb = avgDb.toFloat(),
                peakDb = captureMaxDb.toFloat(),
                location = location,
                spectralEnergy = captureSpectrumSum ?: FloatArray(0),
                labels = labels,
                weighting = state.selectedWeighting.name
            )
        }
        
        _uiState.update { it.copy(isCapturing = false, captureProgress = 0f) }
    }

    /**
     * Remuestrea la señal de audio de 44.1kHz a 16kHz para compatibilidad con el modelo ML.
     */
    private fun resampleTo16kHz(input: FloatArray): FloatArray {
        val outputSize = (input.size * 16000.0 / 44100.0).toInt()
        val output = FloatArray(outputSize)
        for (i in output.indices) {
            val inputIndex = (i * input.size / outputSize).coerceAtMost(input.size - 1)
            output[i] = input[inputIndex]
        }
        return output
    }

    private fun resetCaptureAccumulators() {
        captureDbSum = 0.0
        captureCount = 0
        captureMaxDb = -100.0
        energyLow = 0.0; energyMid = 0.0; energyHigh = 0.0
        captureSpectrumSum = null
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
        audioManager.stopRecording()
        classifierManager.close()
        recordingJob?.cancel()
    }
}
