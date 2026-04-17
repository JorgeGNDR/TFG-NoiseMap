package com.gandara.tfgjorgegandara.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.gandara.tfgjorgegandara.dsp.AudioCaptureManager
import com.gandara.tfgjorgegandara.dsp.FFTCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 1. Creamos un data class para guardar el estado exacto de la pantalla en cada milisegundo
data class AnalyzerState(
    val isRecording: Boolean = false,
    val decibels: Double = 0.0,
    val spectrum: FloatArray = FloatArray(0),
    val offset: Float = 90f
)

class AnalyzerViewModel : ViewModel() {

    private val audioManager = AudioCaptureManager()
    private val fftCalculator = FFTCalculator(1024)

    // StateFlowpara comunicar con la interfaz
    private val _uiState = MutableStateFlow(AnalyzerState())
    val uiState: StateFlow<AnalyzerState> = _uiState.asStateFlow()

    fun startAnalyzing() {
        if (_uiState.value.isRecording) return

        _uiState.update { it.copy(isRecording = true) }

        // Sincronizamos el offset del AudioManager con el estado actual
        audioManager.offset = _uiState.value.offset.toDouble()

        // Encendemos el micro y empezamos a recibir el callback con los datos a toda velocidad
        audioManager.startRecording { audioBuffer, dbTotal ->

            // Pasamos el audio bruto por nuestra calculadora matemática
            val fftResult = fftCalculator.calculateFFT(
                audioSamples = audioBuffer,
                offset = _uiState.value.offset
            )

            // Actualizamos el estado. Jetpack Compose detecta este cambio
            // y actualiza la pantalla
            _uiState.update { currentState ->
                currentState.copy(
                    decibels = dbTotal,
                    spectrum = fftResult
                )
            }
        }
    }

    fun stopAnalyzing() {
        audioManager.stopRecording()
        _uiState.update { it.copy(isRecording = false) }
    }

    // Función para actualizar el offset en la pantalla de calibración
    fun updateCalibrationOffset(newOffset: Float) {
        _uiState.update { it.copy(offset = newOffset) }
        audioManager.offset = newOffset.toDouble()
    }

    // Si el usuario cierra la app o cambia de pantalla,
    // el ViewModel muere y apagamos el micro automáticamente para no espiar ni gastar batería.
    override fun onCleared() {
        super.onCleared()
        audioManager.stopRecording()
    }
}