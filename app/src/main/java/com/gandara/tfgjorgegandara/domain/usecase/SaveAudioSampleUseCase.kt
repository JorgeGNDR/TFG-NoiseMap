package com.gandara.tfgjorgegandara.domain.usecase

import com.gandara.tfgjorgegandara.domain.model.AudioMeasurement
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository

class SaveAudioSampleUseCase(
    private val repository: AudioRepository
) {
    suspend operator fun invoke(measurement: AudioMeasurement): Result<Long> {
        return repository.saveCompleteAudioSample(
            sessionId = measurement.sessionId,
            timestamp = measurement.timestamp,
            durationMs = measurement.durationMs,
            avgDb = measurement.avgDb,
            peakDb = measurement.peakDb,
            latitude = measurement.latitude,
            longitude = measurement.longitude,
            spectralEnergy = measurement.spectralEnergy,
            labels = measurement.detectedSounds,
            dominantFreq = measurement.dominantFrequency,
            weighting = measurement.weighting.name,
            calibrationOffset = measurement.calibrationOffset
        )
    }
}

class CreateMeasurementSessionUseCase(
    private val repository: AudioRepository
) {
    suspend operator fun invoke(startTimestamp: Long): Result<Long> {
        return repository.createMeasurementSession(startTimestamp)
    }
}

class FinishMeasurementSessionUseCase(
    private val repository: AudioRepository
) {
    suspend operator fun invoke(sessionId: Long, endTimestamp: Long, durationMs: Long): Result<Unit> {
        return repository.finishMeasurementSession(sessionId, endTimestamp, durationMs)
    }
}
