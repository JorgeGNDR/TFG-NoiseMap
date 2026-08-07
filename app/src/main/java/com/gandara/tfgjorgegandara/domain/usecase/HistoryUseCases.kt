package com.gandara.tfgjorgegandara.domain.usecase

import com.gandara.tfgjorgegandara.domain.model.AudioSampleRecord
import com.gandara.tfgjorgegandara.domain.model.FullAudioSample
import com.gandara.tfgjorgegandara.domain.model.HistoryContent
import com.gandara.tfgjorgegandara.domain.model.MeasurementSessionHistory
import com.gandara.tfgjorgegandara.domain.repository.HistoryRepository
import com.gandara.tfgjorgegandara.domain.repository.NoiseExplanationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveHistoryUseCase(private val repository: HistoryRepository) {
    operator fun invoke(): Flow<HistoryContent> = combine(
        repository.observeSessions(),
        repository.observeSamples()
    ) { sessions, samples ->
        val samplesBySession = samples
            .filter { it.sessionId != null }
            .groupBy { it.sessionId }
        val knownSessionIds = sessions.mapTo(mutableSetOf()) { it.id }

        HistoryContent(
            sessions = sessions.mapNotNull { session ->
                val sessionSamples = samplesBySession[session.id].orEmpty()
                sessionSamples.takeIf { it.isNotEmpty() }?.let {
                    MeasurementSessionHistory.create(session, it)
                }
            }.sortedByDescending { it.session.startTimestamp },
            standaloneSamples = samples.filter { sample ->
                sample.sessionId == null || sample.sessionId !in knownSessionIds
            }.sortedByDescending { it.timestamp }
        )
    }
}

class GetSampleDetailsUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(sample: AudioSampleRecord): FullAudioSample =
        repository.getSampleDetails(sample)
}

class DeleteSampleUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(sample: AudioSampleRecord) = repository.deleteSample(sample)
}

class DeleteSamplesUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(samples: List<AudioSampleRecord>) = repository.deleteSamples(samples)
}

class DeleteSessionUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(sessionId: Long) = repository.deleteSession(sessionId)
}

class ExplainSampleUseCase(
    private val historyRepository: HistoryRepository,
    private val explanationRepository: NoiseExplanationRepository
) {
    suspend operator fun invoke(details: FullAudioSample): String {
        val explanation = explanationRepository.explainSample(details)
        historyRepository.updateAiExplanation(details.sample.id, explanation)
        return explanation
    }
}

data class HistoryUseCases(
    val observeHistory: ObserveHistoryUseCase,
    val getSampleDetails: GetSampleDetailsUseCase,
    val deleteSample: DeleteSampleUseCase,
    val deleteSamples: DeleteSamplesUseCase,
    val deleteSession: DeleteSessionUseCase,
    val explainSample: ExplainSampleUseCase
)
