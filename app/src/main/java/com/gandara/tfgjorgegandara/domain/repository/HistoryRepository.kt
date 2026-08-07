package com.gandara.tfgjorgegandara.domain.repository

import com.gandara.tfgjorgegandara.domain.model.AudioSampleRecord
import com.gandara.tfgjorgegandara.domain.model.FullAudioSample
import com.gandara.tfgjorgegandara.domain.model.MeasurementSessionRecord
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeSamples(): Flow<List<AudioSampleRecord>>

    fun observeSessions(): Flow<List<MeasurementSessionRecord>>

    suspend fun getSampleDetails(sample: AudioSampleRecord): FullAudioSample

    suspend fun deleteSample(sample: AudioSampleRecord)

    suspend fun deleteSamples(samples: List<AudioSampleRecord>)

    suspend fun deleteSession(sessionId: Long)

    suspend fun updateAiExplanation(sampleId: Long, explanation: String)
}
