package com.gandara.tfgjorgegandara.ai

import com.gandara.tfgjorgegandara.data.local.AudioSample
import com.gandara.tfgjorgegandara.data.local.FrequencyBin
import com.gandara.tfgjorgegandara.data.local.SoundClassification
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoiseExplanationService {
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 1024
                thinkingConfig = thinkingConfig {
                    thinkingBudget = 0
                }
            }
        )

    suspend fun explainSample(
        sample: AudioSample,
        bins: List<FrequencyBin>,
        classifications: List<SoundClassification>
    ): String = withContext(Dispatchers.IO) {
        val responseText = try {
            extractText(model.generateContent(buildPrompt(sample, bins, classifications)))
        } catch (e: ResponseStoppedException) {
            extractText(e.response)
        }

        responseText.ifBlank {
            "No se ha podido generar una explicacion para esta muestra."
        }
    }

    private fun extractText(response: GenerateContentResponse): String {
        return response.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.filterIsInstance<TextPart>()
            ?.filterNot { it.isThought }
            ?.joinToString(" ") { it.text }
            ?.trim()
            .orEmpty()
    }

    private fun buildPrompt(
        sample: AudioSample,
        bins: List<FrequencyBin>,
        classifications: List<SoundClassification>
    ): String {
        val labels = classifications.take(4).joinToString { item ->
            "${item.label} ${(item.probability * 100).toInt()}%"
        }.ifBlank { "sin sonidos claros detectados" }

        val strongestBands = bins
            .sortedByDescending { it.energy }
            .take(5)
            .joinToString { bin ->
                val frequency = AudioRepository.THIRD_OCTAVE_FREQUENCIES.getOrNull(bin.band)
                if (frequency != null) {
                    "${frequency.toInt()} Hz (${bin.energy.toInt()} dB)"
                } else {
                    "banda ${bin.band} (${bin.energy.toInt()} dB)"
                }
            }
            .ifBlank { "sin datos espectrales" }

        return """
            Explica esta medicion de ruido ambiental para una persona sin conocimientos de audio.
            Responde en espanol, en 3 frases breves, sin listas y sin tecnicismos innecesarios.

            Datos: nivel medio ${"%.1f".format(sample.avgDb)} dB(${sample.weighting}), pico ${"%.1f".format(sample.peakDb)} dB, frecuencia dominante ${sample.dominantFreq.toInt()} Hz, sonidos detectados: $labels, bandas con mas energia: $strongestBands.

            Indica si parece bajo, moderado o elevado, que podria causarlo y que significa para el usuario.
        """.trimIndent()
    }
}
