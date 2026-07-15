package com.gandara.tfgjorgegandara.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.gandara.tfgjorgegandara.domain.audio.AudioCaptureSource
import kotlinx.coroutines.*
import kotlin.math.max

/**
 * Gestor de la captura de audio crudo desde el hardware del dispositivo.
 * Se encarga de la configuración del buffer y la ejecución del hilo de lectura.
 */
class AudioCaptureManager(private val fftSize: Int = 4096) : AudioCaptureSource {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val minHardwareBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val safeBufferSizeInBytes = max(minHardwareBuffer, fftSize * 2)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private var captureJob: Job? = null
    private val captureScope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun startRecording(onDataReady: (ShortArray) -> Unit) {
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
                        onDataReady(dataBuffer.clone())
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

    override fun start(onAudioData: (ShortArray) -> Unit) {
        startRecording { samples -> onAudioData(samples) }
    }

    override fun stop() {
        stopRecording()
    }
}
