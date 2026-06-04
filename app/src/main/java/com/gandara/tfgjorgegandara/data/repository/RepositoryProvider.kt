package com.gandara.tfgjorgegandara.data.repository

import android.content.Context
import com.gandara.tfgjorgegandara.data.ai.NoiseExplanationService
import com.gandara.tfgjorgegandara.data.local.AppDatabase
import com.gandara.tfgjorgegandara.domain.repository.AudioRepository
import com.gandara.tfgjorgegandara.domain.repository.HistoryRepository
import com.gandara.tfgjorgegandara.domain.repository.NoiseExplanationRepository

object RepositoryProvider {
    fun audioRepository(context: Context): AudioRepository {
        val db = AppDatabase.getDatabase(context.applicationContext)
        return RoomAudioRepository(
            db.audioSampleDao(),
            db.geoTileDao(),
            db.frequencyBinDao(),
            db.soundClassificationDao()
        )
    }

    fun historyRepository(context: Context): HistoryRepository {
        val db = AppDatabase.getDatabase(context.applicationContext)
        return RoomHistoryRepository(
            db.audioSampleDao(),
            db.frequencyBinDao(),
            db.soundClassificationDao()
        )
    }

    fun noiseExplanationRepository(): NoiseExplanationRepository {
        return NoiseExplanationService()
    }
}
