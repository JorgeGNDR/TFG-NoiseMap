package com.gandara.tfgjorgegandara.data.ml

import android.content.Context
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
    private var resampler: StreamingLinearResampler? = null
    private var sourceSampleRate: Int? = null
    private var inputWindow = FloatArray(0)
    private var inputWriteIndex = 0
    private var collectedSamples = 0
    private val audioBufferLock = Any()
    private val inferenceLock = Any()

    companion object {
        private const val TAG = "SoundClassifierManager"
        private const val MODEL_PATH = "yamnet.tflite"
    }

    data class ClassificationResult(
        val label: String,
        val probability: Float? = null
    ) {
        val displayText: String
            get() = probability?.let { "$label (${(it * 100).toInt()}%)" } ?: label
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
            
            // Tensor y ventana de entrada configurados según los metadatos de YAMNet.
            tensorAudio = classifier?.createInputTensorAudio()
            inputWindow = FloatArray(classifier?.requiredInputBufferSize?.toInt() ?: 0)

            Log.d(TAG, "Motor YAMNet inicializado correctamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al cargar el modelo YAMNet: ${e.message}")
        }
    }

    /**
     * Recibe el mismo audio capturado para el analizador y conserva la última
     * ventana requerida por YAMNet, remuestreada a la frecuencia del modelo.
     */
    fun offerAudio(samples: ShortArray, sampleRate: Int) {
        val currentClassifier = classifier ?: return
        if (inputWindow.isEmpty()) return

        val currentResampler = synchronized(audioBufferLock) {
            if (resampler == null || sourceSampleRate != sampleRate) {
                sourceSampleRate = sampleRate
                resampler = StreamingLinearResampler(
                    sourceSampleRate = sampleRate,
                    targetSampleRate = currentClassifier.requiredTensorAudioFormat.sampleRate
                )
                inputWriteIndex = 0
                collectedSamples = 0
            }
            resampler!!
        }

        val resampledSamples = currentResampler.process(samples)
        synchronized(audioBufferLock) {
            resampledSamples.forEach { sample ->
                inputWindow[inputWriteIndex] = sample
                inputWriteIndex = (inputWriteIndex + 1) % inputWindow.size
                collectedSamples = (collectedSamples + 1).coerceAtMost(inputWindow.size)
            }
        }
    }

    /**
     * Clasifica la última ventana completa recibida desde la captura compartida.
     */
    fun classifyContinuous(): ClassificationResult {
        val currentClassifier = classifier ?: return ClassificationResult("Inicializando...")
        val currentTensor = tensorAudio ?: return ClassificationResult("Cargando...")

        val latestWindow = synchronized(audioBufferLock) {
            if (collectedSamples < inputWindow.size) return ClassificationResult("Analizando...")

            FloatArray(inputWindow.size) { index ->
                inputWindow[(inputWriteIndex + index) % inputWindow.size]
            }
        }

        return try {
            val results = synchronized(inferenceLock) {
                currentTensor.load(latestWindow)
                currentClassifier.classify(currentTensor)
            }
            val topCategory = results.firstOrNull()?.categories?.firstOrNull()

            if (topCategory != null && topCategory.score > 0.3f) {
                ClassificationResult(
                    label = topCategory.label,
                    probability = topCategory.score
                )
            } else {
                ClassificationResult("Analizando...")
            }
        } catch (e: Exception) {
            ClassificationResult("Error al leer datos del micrófono. Comprueba permisos")
        }
    }

    /**
     * Libera los recursos del motor de inferencia y la captura de audio.
     */
    fun close() {
        try {
            classifier?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar recursos del gestor ML.")
        }
    }
}
