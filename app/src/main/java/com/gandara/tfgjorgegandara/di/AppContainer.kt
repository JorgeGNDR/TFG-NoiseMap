package com.gandara.tfgjorgegandara.di

import android.content.Context
import com.gandara.tfgjorgegandara.data.ai.NoiseExplanationService
import com.gandara.tfgjorgegandara.data.audio.AudioCaptureManager
import com.gandara.tfgjorgegandara.data.local.AppDatabase
import com.gandara.tfgjorgegandara.data.location.LocationHelper
import com.gandara.tfgjorgegandara.data.ml.SoundClassifierManager
import com.gandara.tfgjorgegandara.data.repository.RoomAudioRepository
import com.gandara.tfgjorgegandara.data.repository.RoomHistoryRepository
import com.gandara.tfgjorgegandara.data.settings.AppSettings
import com.gandara.tfgjorgegandara.domain.audio.AudioCaptureSource
import com.gandara.tfgjorgegandara.domain.audio.SoundClassifier
import com.gandara.tfgjorgegandara.domain.location.LocationTracker
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository
import com.gandara.tfgjorgegandara.domain.repository.HistoryRepository
import com.gandara.tfgjorgegandara.domain.repository.NoiseExplanationRepository
import com.gandara.tfgjorgegandara.domain.settings.SettingsRepository
import com.gandara.tfgjorgegandara.domain.usecase.DeleteSampleUseCase
import com.gandara.tfgjorgegandara.domain.usecase.DeleteSamplesUseCase
import com.gandara.tfgjorgegandara.domain.usecase.DeleteSessionUseCase
import com.gandara.tfgjorgegandara.domain.usecase.ExplainSampleUseCase
import com.gandara.tfgjorgegandara.domain.usecase.CreateMeasurementSessionUseCase
import com.gandara.tfgjorgegandara.domain.usecase.FinishMeasurementSessionUseCase
import com.gandara.tfgjorgegandara.domain.usecase.GetHeatmapDataUseCase
import com.gandara.tfgjorgegandara.domain.usecase.GetSampleDetailsUseCase
import com.gandara.tfgjorgegandara.domain.usecase.HistoryUseCases
import com.gandara.tfgjorgegandara.domain.usecase.ObserveHistoryUseCase
import com.gandara.tfgjorgegandara.domain.usecase.SaveAudioSampleUseCase

/**
 * Punto de composición: construye implementaciones de datos y las inyecta en
 * los casos de uso consumidos por la capa de presentación.
 */
object AppContainer {
    fun audioCapture(bufferSize: Int): AudioCaptureSource = AudioCaptureManager(bufferSize)

    fun soundClassifier(context: Context): SoundClassifier = SoundClassifierManager(context)

    fun locationTracker(context: Context): LocationTracker = LocationHelper(context)

    fun settings(context: Context): SettingsRepository {
        AppSettings.init(context.applicationContext)
        return AppSettings
    }

    fun saveAudioSample(context: Context): SaveAudioSampleUseCase =
        SaveAudioSampleUseCase(audioRepository(context))

    fun createMeasurementSession(context: Context): CreateMeasurementSessionUseCase =
        CreateMeasurementSessionUseCase(audioRepository(context))

    fun finishMeasurementSession(context: Context): FinishMeasurementSessionUseCase =
        FinishMeasurementSessionUseCase(audioRepository(context))

    fun getHeatmapData(context: Context): GetHeatmapDataUseCase =
        GetHeatmapDataUseCase(audioRepository(context))

    fun historyUseCases(context: Context): HistoryUseCases {
        val historyRepository = historyRepository(context)
        return HistoryUseCases(
            observeHistory = ObserveHistoryUseCase(historyRepository),
            getSampleDetails = GetSampleDetailsUseCase(historyRepository),
            deleteSample = DeleteSampleUseCase(historyRepository),
            deleteSamples = DeleteSamplesUseCase(historyRepository),
            deleteSession = DeleteSessionUseCase(historyRepository),
            explainSample = ExplainSampleUseCase(
                historyRepository,
                noiseExplanationRepository()
            )
        )
    }

    private fun audioRepository(context: Context): AudioRepository {
        val db = AppDatabase.getDatabase(context.applicationContext)
        return RoomAudioRepository(
            db,
            db.measurementSessionDao(),
            db.audioSampleDao(),
            db.frequencyBinDao(),
            db.soundClassificationDao()
        )
    }

    private fun historyRepository(context: Context): HistoryRepository {
        val db = AppDatabase.getDatabase(context.applicationContext)
        return RoomHistoryRepository(
            db.measurementSessionDao(),
            db.audioSampleDao(),
            db.frequencyBinDao(),
            db.soundClassificationDao()
        )
    }

    private fun noiseExplanationRepository(): NoiseExplanationRepository {
        return NoiseExplanationService()
    }
}
