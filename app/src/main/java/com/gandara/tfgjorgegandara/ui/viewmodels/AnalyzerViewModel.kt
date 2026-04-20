package com.gandara.tfgjorgegandara.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.gandara.tfgjorgegandara.dsp.AudioCaptureManager
import com.gandara.tfgjorgegandara.dsp.FFTCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

data class AnalyzerState(
    val isRecording: Boolean = false,
    val decibels: Double = 0.0,
    val peak: Double = 0.0,
    val avg: Double = 0.0,
    val spectrum: FloatArray = FloatArray(0),
    val peakHoldSpectrum: FloatArray = FloatArray(0), // Nueva curva de picos
    val offset: Float = 90f
)

class AnalyzerViewModel : ViewModel() {

    private val audioManager = AudioCaptureManager()
    private val fftCalculator = FFTCalculator(4096)

    private val _uiState = MutableStateFlow(AnalyzerState())
    val uiState: StateFlow<AnalyzerState> = _uiState.asStateFlow()

    private var sumOfSquarePressures = 0.0
    private var dbCount = 0
    private var internalPeak = 0.0
    private var lastUiUpdateTime = 0L
    private val UI_UPDATE_INTERVAL_MS = 50L

    // Array interno para mantener los picos máximos
    private var currentPeakHold = FloatArray(0)
    private val DECAY_FACTOR = 0.98f // Velocidad de caída (más cerca de 1.0 es más lento)

    fun toggleRecording() {
        if (_uiState.value.isRecording) stopAnalyzing() else startAnalyzing()
    }

    fun startAnalyzing() {
        sumOfSquarePressures = 0.0
        dbCount = 0
        internalPeak = 0.0
        lastUiUpdateTime = System.currentTimeMillis()
        currentPeakHold = FloatArray(0)

        _uiState.update { it.copy(isRecording = true, peak = 0.0, avg = 0.0) }
        audioManager.offset = _uiState.value.offset.toDouble()

        audioManager.startRecording { audioBuffer, dbTotal ->
            val fftResult = fftCalculator.calculateFFT(audioSamples = audioBuffer, offset = _uiState.value.offset)

            // Inicializar o actualizar Peak Hold
            if (currentPeakHold.size != fftResult.size) {
                currentPeakHold = fftResult.clone()
            } else {
                for (i in fftResult.indices) {
                    // Si el nuevo valor es mayor, se queda el nuevo. Si no, el viejo cae lentamente.
                    currentPeakHold[i] = max(fftResult[i], currentPeakHold[i] * DECAY_FACTOR)
                    // Aseguramos que no baje del suelo de la gráfica
                    if (currentPeakHold[i] < -20f) currentPeakHold[i] = -20f
                }
            }

            dbCount++
            val linearEnergy = 10.0.pow(dbTotal / 10.0)
            sumOfSquarePressures += linearEnergy
            val linearAverage = sumOfSquarePressures / dbCount
            val trueAvgDb = 10.0 * log10(linearAverage)
            internalPeak = max(internalPeak, dbTotal)

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUiUpdateTime >= UI_UPDATE_INTERVAL_MS) {
                lastUiUpdateTime = currentTime
                _uiState.update { currentState ->
                    currentState.copy(
                        decibels = dbTotal,
                        peak = internalPeak,
                        avg = trueAvgDb,
                        spectrum = fftResult.clone(),
                        peakHoldSpectrum = currentPeakHold.clone()
                    )
                }
            }
        }
    }

    fun stopAnalyzing() {
        audioManager.stopRecording()
        _uiState.update { it.copy(isRecording = false) }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.stopRecording()
    }
}