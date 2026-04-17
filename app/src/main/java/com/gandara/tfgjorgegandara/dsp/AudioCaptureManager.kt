package com.gandara.tfgjorgegandara.dsp

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.log10
import kotlin.math.sqrt

class AudioCaptureManager {
    //Configuración del audio
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    //Buffer
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    var offset = 90.0

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
            bufferSize
        )
        val dataBuffer = ShortArray(1024)

        isRecording = true
        audioRecord?.startRecording()

        // Lanzamos corrutina para leer los datos del audio en segundo plano
        captureJob = captureScope.launch {
            while (isActive && isRecording) {
                val bytesRead = audioRecord?.read(dataBuffer, 0, dataBuffer.size) ?: 0

                if (bytesRead > 0) {
                    // Cálculo de decibelios
                    val db = calculateDb(dataBuffer)

                    // Enviamos datos (buffer y db) a la UI
                    onDataReady(dataBuffer.clone(), db)
                }
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        // Cancelamos la corrutina limpiamente
        captureJob?.cancel()
        captureJob = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    // Cálculo de decibelios
    private fun calculateDb(samples: ShortArray): Double {
        var sum = 0.0
        for (sample in samples) {
            sum += (sample * sample).toDouble()
        }
        //RMS (Root Mean Square) es el promedio de la energía de la onda (Amplitud)
        val rms = sqrt(sum / samples.size)

        //Fórmula decibelios: 20 * log10(RMS)
        // Usamos 32767.0 como referencia porque es el valor máximo de un Short (16 bits)
        return if (rms > 0) 20 * log10(rms / 32767.0) + offset else 0.0
    }
}
