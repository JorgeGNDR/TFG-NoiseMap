package com.gandara.tfgjorgegandara.domain.repository

import com.gandara.tfgjorgegandara.domain.model.AudioSampleRecord
import com.gandara.tfgjorgegandara.domain.model.FullAudioSample
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeSamples(): Flow<List<AudioSampleRecord>>

    suspend fun getSampleDetails(sample: AudioSampleRecord): FullAudioSample

    suspend fun deleteSample(sample: AudioSampleRecord)

    suspend fun updateAiExplanation(sampleId: Long, explanation: String)
}
