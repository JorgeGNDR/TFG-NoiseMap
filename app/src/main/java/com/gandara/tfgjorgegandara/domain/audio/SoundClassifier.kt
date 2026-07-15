package com.gandara.tfgjorgegandara.domain.audio

data class SoundClassificationResult(
    val label: String,
    val probability: Float? = null
) {
    val displayText: String
        get() = probability?.let { "$label (${(it * 100).toInt()}%)" } ?: label
}

interface SoundClassifier {
    fun offerAudio(samples: ShortArray, sampleRate: Int)
    fun classifyLatest(): SoundClassificationResult
    fun close()
}
