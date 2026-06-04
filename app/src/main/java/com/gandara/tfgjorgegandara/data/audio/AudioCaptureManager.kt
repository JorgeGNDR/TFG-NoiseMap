package com.gandara.tfgjorgegandara.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Gestor de la captura de audio crudo desde el hardware del dispositivo.
 * Se encarga de la configuración del buffer y la ejecución del hilo de lectura.
 */
class AudioCaptureManager(private val fftSize: Int = 4096) {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val minHardwareBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val safeBufferSizeInBytes = max(minHardwareBuffer, fftSize * 2)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    var offset = 100.0

    private var captureJob: Job? = null
    private val captureScope = CoroutineScope(Dispatchers.IO)

    /**
     * Inicia la captura de audio de forma asíncrona.
     * @param onDataReady Callback que se invoca cada vez que se llena un buffer con datos de audio y su nivel en dB.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(onDataReady: (ShortArray, Double) -> Unit) {
        if (isRecording) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                safeBufferSizeInBytes
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioCaptureManager", "Error al inicializar el hardware de audio.")
                return
            }

            val dataBuffer = ShortArray(fftSize)
            audioRecord?.startRecording()
            isRecording = true

            captureJob = captureScope.launch {
                while (isActive && isRecording) {
                    val samplesRead = audioRecord?.read(dataBuffer, 0, dataBuffer.size) ?: 0
                    if (samplesRead == dataBuffer.size) {
                        val db = calculateDb(dataBuffer)
                        onDataReady(dataBuffer.clone(), db)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioCaptureManager", "Excepción durante la captura: ${e.message}")
            stopRecording()
        }
    }

    /**
     * Detiene la captura y libera los recursos del sistema.
     */
    fun stopRecording() {
        isRecording = false
        captureJob?.cancel()
        captureJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("AudioCaptureManager", "Fallo al liberar AudioRecord: ${e.message}")
        }
        audioRecord = null
    }

    /**
     * Calcula el nivel de presión sonora (dB SPL aproximado) utilizando el valor RMS de las muestras.
     */
    private fun calculateDb(samples: ShortArray): Double {
        var sumOfSquares = 0.0
        for (sample in samples) {
            // Normalización de la muestra de 16 bits al rango [-1.0, 1.0]
            val normalized = sample / 32768.0
            sumOfSquares += normalized * normalized
        }
        
        // Root Mean Square (Valor Eficaz)
        val rms = sqrt(sumOfSquares / samples.size)

        // Conversión a escala logarítmica (decibelios) aplicando el offset de calibración
        return if (rms > 1e-9) {
            20 * log10(rms) + offset
        } else {
            0.0
        }
    }
}
