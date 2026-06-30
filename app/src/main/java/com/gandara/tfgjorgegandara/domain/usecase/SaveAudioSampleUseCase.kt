package com.gandara.tfgjorgegandara.domain.usecase

import com.gandara.tfgjorgegandara.domain.model.AudioMeasurement
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository

class SaveAudioSampleUseCase(
    private val repository: AudioRepository
) {
    suspend operator fun invoke(measurement: AudioMeasurement): Result<Long> {
        return repository.saveCompleteAudioSample(
            avgDb = measurement.avgDb,
            peakDb = measurement.peakDb,
            latitude = measurement.latitude,
            longitude = measurement.longitude,
            spectralEnergy = measurement.spectralEnergy,
            labels = measurement.detectedSounds,
            dominantFreq = measurement.dominantFrequency,
            weighting = measurement.weighting.name
        )
    }
}
