package com.gandara.tfgjorgegandara.data.ai

import com.gandara.tfgjorgegandara.domain.model.AudioSampleRecord
import com.gandara.tfgjorgegandara.domain.model.FrequencyBandEnergy
import com.gandara.tfgjorgegandara.domain.model.FullAudioSample
import com.gandara.tfgjorgegandara.domain.model.SoundDetection
import com.gandara.tfgjorgegandara.domain.model.ThirdOctaveBands
import com.gandara.tfgjorgegandara.domain.repository.NoiseExplanationRepository
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

class NoiseExplanationService : NoiseExplanationRepository {
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

    override suspend fun explainSample(sample: FullAudioSample): String = withContext(Dispatchers.IO) {
        val responseText = try {
            extractText(model.generateContent(buildPrompt(sample.sample, sample.bins, sample.classifications)))
        } catch (e: ResponseStoppedException) {
            extractText(e.response)
        }

        responseText.ifBlank {
            "No se ha podido generar una explicación para esta muestra."
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
        sample: AudioSampleRecord,
        bins: List<FrequencyBandEnergy>,
        classifications: List<SoundDetection>
    ): String {
        val labels = classifications.take(4).joinToString { item ->
            "${item.label} ${(item.probability * 100).toInt()}%"
        }.ifBlank { "sin sonidos claros detectados" }

        val strongestBands = bins
            .sortedByDescending { it.energy }
            .take(5)
            .joinToString { bin ->
                val frequency = ThirdOctaveBands.CENTER_FREQUENCIES_HZ.getOrNull(bin.band)
                if (frequency != null) {
                    "${frequency.toInt()} Hz (${bin.energy.toInt()} dB)"
                } else {
                    "banda ${bin.band} (${bin.energy.toInt()} dB)"
                }
            }
            .ifBlank { "sin datos espectrales" }

        return """
            Explica esta medición de ruido ambiental para una persona sin conocimientos de audio.
            Responde en español, en 3 frases breves, sin listas y sin tecnicismos innecesarios.

            Datos: nivel medio ${"%.1f".format(sample.avgDb)} dB(${sample.weighting}), pico ${"%.1f".format(sample.peakDb)} dB, frecuencia dominante ${sample.dominantFreq.toInt()} Hz, sonidos detectados: $labels, bandas con más energía: $strongestBands.

            Indica si parece bajo, moderado o elevado, qué podría causarlo y qué podría implicar para el usuario si la exposición a niveles similares se mantiene en el tiempo.
        """.trimIndent()
    }
}
