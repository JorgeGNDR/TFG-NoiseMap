package com.gandara.tfgjorgegandara.domain.usecase

import com.gandara.tfgjorgegandara.domain.model.AudioSampleRecord
import com.gandara.tfgjorgegandara.domain.model.FullAudioSample
import com.gandara.tfgjorgegandara.domain.repository.HistoryRepository
import com.gandara.tfgjorgegandara.domain.repository.NoiseExplanationRepository
import kotlinx.coroutines.flow.Flow

class ObserveHistoryUseCase(private val repository: HistoryRepository) {
    operator fun invoke(): Flow<List<AudioSampleRecord>> = repository.observeSamples()
}

class GetSampleDetailsUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(sample: AudioSampleRecord): FullAudioSample =
        repository.getSampleDetails(sample)
}

class DeleteSampleUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(sample: AudioSampleRecord) = repository.deleteSample(sample)
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
    val explainSample: ExplainSampleUseCase
)
