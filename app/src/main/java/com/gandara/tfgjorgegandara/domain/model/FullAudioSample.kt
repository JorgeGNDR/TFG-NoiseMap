package com.gandara.tfgjorgegandara.domain.model

data class FullAudioSample(
    val sample: AudioSampleRecord,
    val bins: List<FrequencyBandEnergy>,
    val classifications: List<SoundDetection>
)
