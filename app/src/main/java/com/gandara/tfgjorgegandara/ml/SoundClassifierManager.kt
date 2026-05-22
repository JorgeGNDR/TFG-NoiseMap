package com.gandara.tfgjorgegandara.ml

import android.content.Context
import android.media.AudioRecord
import android.util.Log
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.core.BaseOptions

/**
 * Gestor del motor de inferencia TensorFlow Lite para la clasificación de sonidos ambientales.
 * Utiliza el modelo pre-entrenado YAMNet.
 */
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
                .setMaxResults(2)
                .build()

            // Inicialización del motor de TFLite
            classifier = AudioClassifier.createFromFileAndOptions(context, MODEL_PATH, options)
            
            // Configuración automatizada del buffer de audio conforme a los requisitos del modelo (16kHz)
            tensorAudio = classifier?.createInputTensorAudio()
            audioRecord = classifier?.createAudioRecord()

            Log.d(TAG, "Motor de inteligencia artificial inicializado correctamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al cargar el modelo YAMNet: ${e.message}")
        }
    }

    /**
     * Inicia la captura de audio en el hilo de procesamiento de audio.
     */
    fun start() {
        try {
            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo activar la captura para el motor ML: ${e.message}")
        }
    }

    /**
     * Detiene la captura de audio.
     */
    fun stop() {
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener la captura ML.")
        }
    }

    /**
     * Realiza la clasificación del sonido actual leyendo directamente de su propio flujo de audio.
     * Este método garantiza que YAMNet reciba el contexto completo (aprox 1s) que necesita.
     */
    fun classifyContinuous(): String {
        val currentClassifier = classifier ?: return "Inicializando..."
        val currentTensor = tensorAudio ?: return "Cargando..."

        return try {
            // Lee los datos del micrófono interno configurado por TensorFlow a 16kHz
            currentTensor.load(audioRecord)
            val results = currentClassifier.classify(currentTensor)
            val topCategory = results.firstOrNull()?.categories?.firstOrNull()

            if (topCategory != null && topCategory.score > 0.3f) {
                "${topCategory.label} (${(topCategory.score * 100).toInt()}%)"
            } else {
                "NULL"
            }
        } catch (e: Exception) {
            "Analizando..."
        }
    }

    /**
     * Realiza la clasificación de un fragmento de audio específico (usado en post-procesamiento).
     */
    fun classify(resampled: FloatArray): String {
        val currentClassifier = classifier ?: return "Inicializando..."
        val currentTensor = tensorAudio ?: return "Error de memoria"

        return try {
            currentTensor.load(resampled, 0, resampled.size)
            val results = currentClassifier.classify(currentTensor)
            val topCategory = results.firstOrNull()?.categories?.firstOrNull()

            if (topCategory != null) {
                "${topCategory.label} (${(topCategory.score * 100).toInt()}%)"
            } else {
                "NULL"
            }
        } catch (e: Exception) {
            "Analizando..."
        }
    }

    /**
     * Libera los recursos del motor de inferencia y la captura de audio.
     */
    fun close() {
        try {
            stop()
            audioRecord?.release()
            classifier?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar recursos del gestor ML.")
        }
    }
}
