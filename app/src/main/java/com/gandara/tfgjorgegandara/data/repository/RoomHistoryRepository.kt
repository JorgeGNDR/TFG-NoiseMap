package com.gandara.tfgjorgegandara.data.repository

import com.gandara.tfgjorgegandara.data.local.AudioSample
import com.gandara.tfgjorgegandara.data.local.AudioSampleDao
import com.gandara.tfgjorgegandara.data.local.FrequencyBin
import com.gandara.tfgjorgegandara.data.local.FrequencyBinDao
import com.gandara.tfgjorgegandara.data.local.SoundClassification
import com.gandara.tfgjorgegandara.data.local.SoundClassificationDao
import com.gandara.tfgjorgegandara.domain.model.AudioSampleRecord
import com.gandara.tfgjorgegandara.domain.model.FrequencyBandEnergy
import com.gandara.tfgjorgegandara.domain.model.FullAudioSample
import com.gandara.tfgjorgegandara.domain.model.SoundDetection
import com.gandara.tfgjorgegandara.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomHistoryRepository(
    private val audioSampleDao: AudioSampleDao,
    private val frequencyBinDao: FrequencyBinDao,
    private val soundClassificationDao: SoundClassificationDao
) : HistoryRepository {

    override fun observeSamples(): Flow<List<AudioSampleRecord>> {
        return audioSampleDao.getAllSamples().map { samples ->
            samples.map { it.toDomain() }
        }
    }

    override suspend fun getSampleDetails(sample: AudioSampleRecord): FullAudioSample {
        val bins = frequencyBinDao.getBinsForSample(sample.id).map { it.toDomain() }
        val classifications = soundClassificationDao.getClassificationsForSample(sample.id).map { it.toDomain() }
        return FullAudioSample(sample, bins, classifications)
    }

    override suspend fun deleteSample(sample: AudioSampleRecord) {
        audioSampleDao.deleteSample(sample.toEntity())
    }

    override suspend fun updateAiExplanation(sampleId: Long, explanation: String) {
        audioSampleDao.updateAiExplanation(sampleId, explanation)
    }
}

private fun AudioSample.toDomain(): AudioSampleRecord {
    return AudioSampleRecord(
        id = id,
        sessionId = sessionId,
        timestamp = timestamp,
        durationMs = durationMs,
        latitude = latitude,
        longitude = longitude,
        avgDb = avgDb,
        peakDb = peakDb,
        dominantFreq = dominantFreq,
        weighting = weighting,
        calibrationOffset = calibrationOffset,
        aiExplanation = aiExplanation
    )
}

private fun AudioSampleRecord.toEntity(): AudioSample {
    return AudioSample(
        id = id,
        sessionId = sessionId,
        timestamp = timestamp,
        durationMs = durationMs,
        latitude = latitude,
        longitude = longitude,
        avgDb = avgDb,
        peakDb = peakDb,
        dominantFreq = dominantFreq,
        weighting = weighting,
        calibrationOffset = calibrationOffset,
        aiExplanation = aiExplanation
    )
}

private fun FrequencyBin.toDomain(): FrequencyBandEnergy {
    return FrequencyBandEnergy(
        sampleId = sampleId,
        band = band,
        energy = energy
    )
}

private fun SoundClassification.toDomain(): SoundDetection {
    return SoundDetection(
        sampleId = sampleId,
        label = label,
        probability = probability
    )
}
