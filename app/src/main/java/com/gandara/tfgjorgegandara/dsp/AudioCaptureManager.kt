package com.gandara.tfgjorgegandara.dsp

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

class AudioCaptureManager {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val FFT_SIZE = 4096

    private val minHardwareBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val safeBufferSizeInBytes = max(minHardwareBuffer, FFT_SIZE * 2)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    var offset = 100.0

    private var captureJob: Job? = null
    private val captureScope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun startRecording(onDataReady: (ShortArray, Double) -> Unit) {
        if (isRecording) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            safeBufferSizeInBytes
        )

        val dataBuffer = ShortArray(FFT_SIZE)
        isRecording = true
        audioRecord?.startRecording()

        captureJob = captureScope.launch {
            while (isActive && isRecording) {
                val samplesRead = audioRecord?.read(dataBuffer, 0, dataBuffer.size) ?: 0
                if (samplesRead == dataBuffer.size) {
                    val db = calculateDb(dataBuffer)
                    onDataReady(dataBuffer.clone(), db)
                }
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        captureJob?.cancel()
        captureJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    /**
     * Calcula el nivel de presión sonora (dB SPL aproximado) usando RMS.
     * Es la referencia estándar para el medidor numérico principal.
     */
    private fun calculateDb(samples: ShortArray): Double {
        var sumOfSquares = 0.0
        for (sample in samples) {
            // Normalizamos la muestra de 16 bits a rango -1.0 a 1.0
            val normalized = sample / 32768.0
            sumOfSquares += normalized * normalized
        }
        
        // Valor eficaz (Root Mean Square)
        val rms = sqrt(sumOfSquares / samples.size)

        // Conversión a decibelios con el offset de calibración
        return if (rms > 1e-9) { // Evitar log10 de cero o valores ínfimos
            20 * log10(rms) + offset
        } else {
            0.0
        }
    }
}