package com.gandara.tfgjorgegandara.ml

import android.content.Context
import android.media.AudioRecord
import android.util.Log
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.core.BaseOptions

class SoundClassifierManager(context: Context) {
    private var classifier: AudioClassifier? = null
    private var tensorAudio: TensorAudio? = null
    private var audioRecord: AudioRecord? = null

    companion object {
        private const val TAG = "SoundClassifierManager"
        private const val MODEL_PATH = "yamnet.tflite"
    }

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setNumThreads(2)
                .build()

            val options = AudioClassifier.AudioClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(0.3f)
                .setMaxResults(3)
                .build()

            classifier = AudioClassifier.createFromFileAndOptions(context, MODEL_PATH, options)
            
            // Creamos el Tensor y el AudioRecord configurados AUTOMÁTICAMENTE para el modelo (16kHz)
            tensorAudio = classifier?.createInputTensorAudio()
            audioRecord = classifier?.createAudioRecord()

            Log.d(TAG, "YAMNet cargado y AudioRecord listo a 16kHz")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar el modelo YAMNet. ¿Está el archivo en assets?", e)
        }
    }

    fun start() {
        try {
            audioRecord?.startRecording()
            Log.d(TAG, "Grabación para IA iniciada")
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo iniciar la grabación para IA", e)
        }
    }

    fun stop() {
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener grabación", e)
        }
    }

    fun classify(resampled: FloatArray): String {
        val currentClassifier = classifier ?: return "Iniciando..."
        val currentRecord = audioRecord ?: return "Error Micro"
        val currentTensor = tensorAudio ?: return "Error Memoria"

        return try {
            currentTensor.load(currentRecord)
            val results = currentClassifier.classify(currentTensor)
            val topCategory = results.firstOrNull()?.categories?.firstOrNull()

            if (topCategory != null) {
                "${topCategory.label} (${(topCategory.score * 100).toInt()}%)"
            } else {
                "RUIDO DE FONDO"
            }
        } catch (e: Exception) {
            "Analizando..."
        }
    }

    fun close() {
        try {
            stop()
            audioRecord?.release()
            classifier?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar el manager", e)
        }
    }
}
