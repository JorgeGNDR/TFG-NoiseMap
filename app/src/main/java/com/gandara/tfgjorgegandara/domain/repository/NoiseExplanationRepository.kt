package com.gandara.tfgjorgegandara.domain.repository

import com.gandara.tfgjorgegandara.domain.model.FullAudioSample

interface NoiseExplanationRepository {
    suspend fun explainSample(sample: FullAudioSample): String
}
